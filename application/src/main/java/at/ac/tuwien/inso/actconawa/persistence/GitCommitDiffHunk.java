package at.ac.tuwien.inso.actconawa.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.context.annotation.Lazy;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "commit_diff_hunk")
public class GitCommitDiffHunk implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "new_start_line")
    private Integer newStartLine;

    @Column(name = "new_line_count")
    private Integer newLineCount;

    @Column(name = "old_start_line")
    private Integer oldStartLine;

    @Column(name = "old_line_count")
    private Integer oldLineCount;

    @ManyToOne
    @JoinColumn(name = "diff_file_id")
    private GitCommitDiffFile diffFile;

    @Lazy
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "hunk_dependency_commit",
            joinColumns = @JoinColumn(name = "hunk_id"),
            inverseJoinColumns = @JoinColumn(name = "commit_id"))
    private List<GitCommit> dependencies;

    @Transient
    private Set<String> dependencyCommitShaSet;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getNewStartLine() {
        return newStartLine;
    }

    public void setNewStartLine(Integer newStartLine) {
        this.newStartLine = newStartLine;
    }

    public Integer getNewLineCount() {
        return newLineCount;
    }

    public void setNewLineCount(Integer newLineCount) {
        this.newLineCount = newLineCount;
    }

    public Integer getOldStartLine() {
        return oldStartLine;
    }

    public void setOldStartLine(Integer oldStartLine) {
        this.oldStartLine = oldStartLine;
    }

    public Integer getOldLineCount() {
        return oldLineCount;
    }

    public void setOldLineCount(Integer oldLineCount) {
        this.oldLineCount = oldLineCount;
    }

    public GitCommitDiffFile getDiffFile() {
        return diffFile;
    }

    public void setDiffFile(GitCommitDiffFile diffFile) {
        this.diffFile = diffFile;
    }

    public List<GitCommit> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<GitCommit> dependencies) {
        this.dependencies = dependencies;
    }

    public Set<String> getDependencyCommitShaSet() {
        return dependencyCommitShaSet;
    }

    public void setDependencyCommitShaSet(Set<String> dependencyCommitShaSet) {
        this.dependencyCommitShaSet = dependencyCommitShaSet;
    }
}
