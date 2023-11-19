package at.ac.tuwien.inso.actconawa.service;

import at.ac.tuwien.inso.actconawa.events.CommitIndexingDoneEvent;
import at.ac.tuwien.inso.actconawa.persistence.GitCommit;
import at.ac.tuwien.inso.actconawa.persistence.GitCommitDiffFile;
import at.ac.tuwien.inso.actconawa.persistence.GitCommitDiffHunk;
import at.ac.tuwien.inso.actconawa.persistence.GitCommitRelationship;
import at.ac.tuwien.inso.actconawa.repository.GitCommitDiffFileRepository;
import at.ac.tuwien.inso.actconawa.repository.GitCommitDiffHunkRepository;
import at.ac.tuwien.inso.actconawa.repository.GitCommitRelationshipRepository;
import com.github.javaparser.JavaParser;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import jakarta.annotation.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.patch.Patch;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.github.javaparser.ParseStart.COMPILATION_UNIT;
import static com.github.javaparser.Providers.provider;


@Service
public class GitDependencyIndexingService {

    private static final Logger LOG = LoggerFactory.getLogger(GitDependencyIndexingService.class);

    private final Git git;

    private final GitCommitService gitCommitService;

    private final GitDiffService gitDiffService;

    private final GitCommitDiffHunkRepository gitDiffHunkRepository;

    private final GitCommitDiffFileRepository gitCommitDiffFileRepository;

    private final GitCommitRelationshipRepository gitCommitRelationshipRepository;

    public GitDependencyIndexingService(Git git, GitCommitService gitCommitService, GitDiffService gitDiffService, GitCommitDiffHunkRepository gitDiffHunkRepository, GitCommitDiffFileRepository gitCommitDiffFileRepository, GitCommitRelationshipRepository gitCommitRelationshipRepository) {
        this.git = git;
        this.gitCommitService = gitCommitService;
        this.gitDiffService = gitDiffService;
        this.gitDiffHunkRepository = gitDiffHunkRepository;
        this.gitCommitDiffFileRepository = gitCommitDiffFileRepository;
        this.gitCommitRelationshipRepository = gitCommitRelationshipRepository;

    }

    @EventListener(CommitIndexingDoneEvent.class)
    @Transactional
    public void indexDependencies() {
        LOG.info("Start indexing dependency information");

        var commitDiffFiles = new ArrayList<GitCommitDiffFile>();
        var commitCache = new HashMap<String, GitCommit>();
        // Generic commits
        gitCommitRelationshipRepository.findAll().forEach(cr -> {
            commitCache.putIfAbsent(cr.getChild().getSha(), cr.getChild());
            commitCache.putIfAbsent(cr.getParent().getSha(), cr.getParent());
            commitDiffFiles.addAll(processDiffData(cr));
        });
        // Root commits
        gitCommitRelationshipRepository.findRootCommits().forEach(c -> {
            commitCache.putIfAbsent(c.getSha(), c);
            commitDiffFiles.addAll(processDiffData(c));
        });
        // TODO: Empty commits?

        gitDiffHunkRepository.saveAll(
                commitDiffFiles.stream()
                        .map(GitCommitDiffFile::getGitCommitDiffHunks)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .map(h -> {
                            if (!CollectionUtils.isEmpty(h.getDependencyCommitShaSet())) {
                                h.setDependencies(new ArrayList<>());
                                h.getDependencyCommitShaSet()
                                        .forEach(x -> h.getDependencies().add(commitCache.get(x)));
                            }
                            return h;
                        })
                        .collect(Collectors.toList()));

        gitCommitDiffFileRepository.saveAll(commitDiffFiles);

        indexMethods();
        LOG.info("Indexing dependency info done");
    }

    private List<GitCommitDiffFile> processDiffData(GitCommit gitCommit) {
        var commit = gitCommitService
                .getRevCommitByGitCommitId(gitCommit.getId());
        return processDiffData(commit, null, null);
    }

    private List<GitCommitDiffFile> processDiffData(GitCommitRelationship gitCommitRelationship) {
        var commit = gitCommitService
                .getRevCommitByGitCommitId(gitCommitRelationship.getChild().getId());
        var parentCommit = gitCommitService
                .getRevCommitByGitCommitId(gitCommitRelationship.getParent().getId());
        return processDiffData(commit, parentCommit, gitCommitRelationship);

    }

    private List<GitCommitDiffFile> processDiffData(
            RevCommit commit,
            @Nullable RevCommit parentCommit,
            @Nullable GitCommitRelationship gitCommitRelationship
    ) {
        try (var gitObjectReader = git.getRepository().newObjectReader()) {
            var p = new Patch();
            if (parentCommit != null) {
                p.parse(new ByteArrayInputStream(gitDiffService.getDiff(commit, parentCommit)
                        .getBytes()));
            } else {
                p.parse(new ByteArrayInputStream(gitDiffService.getDiff(commit)
                        .getBytes()));
            }
            var entities = new ArrayList<GitCommitDiffFile>();
            for (FileHeader fileHeader : p.getFiles()) {
                LOG.info("Collecting hunk information for {} ", fileHeader.getNewPath());
                var entity = new GitCommitDiffFile();
                entity.setCommitRelationship(gitCommitRelationship);
                entity.setNewFilePath(fileHeader.getNewPath());
                entity.setNewFileObjectId(resolveObjectId(gitObjectReader,
                        fileHeader.getNewId()).getName());
                entity.setOldFilePath(
                        fileHeader.getChangeType() == DiffEntry.ChangeType.ADD
                                ? null
                                : fileHeader.getOldPath()
                );
                if (fileHeader.getChangeType() != DiffEntry.ChangeType.ADD) {
                    entity.setOldFileObjectId(resolveObjectId(gitObjectReader,
                            fileHeader.getOldId()).getName());
                }
                entity.setChangeType(fileHeader.getChangeType());
                entities.add(entity);
                // Check for parentCommit != null is not necessary, since root commits must be ADD.
                // However, to not show warnings in the ide, this check is added.
                if (fileHeader.getChangeType() != DiffEntry.ChangeType.ADD
                        && parentCommit != null) {
                    entity.setGitCommitDiffHunks(new ArrayList<>());
                    var blame = git.blame()
                            .setStartCommit(parentCommit.getId())
                            .setFollowFileRenames(true)
                            .setFilePath(fileHeader.getOldPath())
                            .call();
                    for (HunkHeader hunk : fileHeader.getHunks()) {
                        LOG.info("Processing {} {}", hunk.toString(), commit.getId().getName());
                        var hunkEntity = new GitCommitDiffHunk();
                        hunkEntity.setNewStartLine(hunk.getNewStartLine());
                        hunkEntity.setNewLineCount(hunk.getNewLineCount());
                        hunkEntity.setOldStartLine(hunk.getOldImage().getStartLine());
                        hunkEntity.setOldLineCount(hunk.getOldImage().getLineCount());
                        hunkEntity.setDependencyCommitShaSet(new HashSet<>());
                        hunkEntity.setDiffFile(entity);
                        entity.getGitCommitDiffHunks().add(hunkEntity);
                        blame.computeRange(hunk.getOldImage().getStartLine(),
                                hunk.getOldImage().getLineCount());
                        var line = hunk.getOldImage().getStartLine() - 1;
                        var relatedAncestorCommits = new HashSet<RevCommit>();
                        for (var i = 0; i < hunk.getOldImage().getLineCount(); ) {
                            var srcCommit = blame.getSourceCommit(line + i++);
                            relatedAncestorCommits.add(srcCommit);
                            hunkEntity.getDependencyCommitShaSet().add(srcCommit.getName());
                        }
                        LOG.info("{} {} depends on {}",
                                hunk,
                                commit.getId().getName(),
                                relatedAncestorCommits.stream().map(RevCommit::getName).collect(
                                        Collectors.joining(",")));
                    }
                }
            }
            return entities;
        } catch (GitAPIException | IOException e) {
            // No information can be gathered for this commit
            LOG.error("Collecting modified/added files between {} and {} failed.",
                    commit.getId().getName(), parentCommit.getId().getName(), e
            );
            return new ArrayList<>();
        }
    }

    public void indexMethods() {
        for (GitCommitDiffFile commitDiffFile : this.gitCommitDiffFileRepository.findAll()) {
            try (var or = git.getRepository().newObjectReader()) {
                var ol = or.open(ObjectId.fromString(commitDiffFile.getNewFileObjectId()));
                var file = new String(ol.getBytes());
                LOG.debug("Successfully loaded file {} at {}",
                        commitDiffFile.getNewFilePath(),
                        commitDiffFile.getCommitRelationship().getChild().getSha());
                if (commitDiffFile.getNewFilePath().endsWith(".java")) {
                    new JavaParser().parse(COMPILATION_UNIT, provider(file)).ifSuccessful(cu ->
                            cu.getTypes().forEach(typeDeclaration -> {
                                Optional.ofNullable(commitDiffFile.getGitCommitDiffHunks())
                                        .orElse(List.of())
                                        .forEach(gitCommitDiffHunk -> {
                                            var start = gitCommitDiffHunk.getNewStartLine();
                                            var end = gitCommitDiffHunk.getNewStartLine()
                                                    + gitCommitDiffHunk.getNewLineCount();
                                            LOG.debug("Find whats happening between {} and {}",
                                                    start,
                                                    end);
                                            // TODO: More than depth == 1?
                                            var map = new TreeMap<Integer, String>();
                                            typeDeclaration.getChildNodes().stream()
                                                    .map(x -> x.getClass().getName()
                                                            + " "
                                                            + x.getRange())
                                                    .forEach(LOG::trace);
                                            for (Node x : typeDeclaration.getChildNodes()) {
                                                var isInRange = x.getRange()
                                                        .map(range -> range
                                                                .overlapsWith(new Range(
                                                                        new Position(start, 0),
                                                                        new Position(end,
                                                                                Integer.MAX_VALUE))))
                                                        .orElse(false);
                                                LOG.debug("{} is in range {}",
                                                        x.getClass().getName(),
                                                        isInRange);
                                            }


                                        });
                            })
                    );
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private ObjectId resolveObjectId(
            ObjectReader objectReader,
            AbbreviatedObjectId abbreviatedObjectId
    ) throws IOException {
        return objectReader.resolve(abbreviatedObjectId)
                .stream()
                .findFirst()
                .orElseThrow(FileNotFoundException::new);
    }

}
