import { Injectable } from '@angular/core';
import {
  GitBranchControllerService,
  GitBranchDto,
  GitBranchTrackingStatusDto,
  GitCommitControllerService,
  GitCommitDiffCodeChangeDto,
  GitCommitDiffFileDto,
  GitCommitDiffHunkDto,
  GitCommitDiffLineChangeDto,
  GitCommitDto,
  GitDiffControllerService
} from "../../api";
import { EMPTY, expand, filter, lastValueFrom, mergeMap, Observable, of, tap } from "rxjs";
import { CompositeKeyMap } from "../utils/CompositeKeyMap";

@Injectable({
  providedIn: 'root'
})
export class GitService {

  private readonly BRANCH_PAGE_SIZE = 10;

  private readonly BRANCH_TRACKING_PAGE_SIZE = 50;

  private readonly COMMIT_QUERY_DEPTH = 3;

  private branchById = new Map<string, GitBranchDto>;

  private commitById = new Map<string, GitCommitDto>;

  private branchIdsByCommitId = new Map<string, string[]>;

  private commitDiffFilesByCommitIds = new CompositeKeyMap<string, GitCommitDiffFileDto[]>;

  private trackingStatusByBranchIds = new CompositeKeyMap<string, GitBranchTrackingStatusDto>;

  private changedCodeByCommitDiffFileId =
          new Map<string, GitCommitDiffCodeChangeDto[]>;

  private changedLinesByCommitDiffFileId =
          new Map<string, GitCommitDiffLineChangeDto[]>;

  private hunksByCommitDiffFileId =
          new Map<string, GitCommitDiffHunkDto[]>;

  constructor(
          private gitCommitService: GitCommitControllerService,
          private gitBranchService: GitBranchControllerService,
          private gitDiffControllerService: GitDiffControllerService
  ) {
  }

  async getCommits(): Promise<GitCommitDto[]> {
    if (this.commitById.size === 0) {
      await this.loadCommitsForBranches();
    }
    return Array.from(this.commitById.values());
  }

  async getCommitById(commitId: string): Promise<GitCommitDto | undefined> {
    if (this.commitById.size === 0) {
      await this.getCommits();
    }
    return this.commitById.get(commitId);
  }

  async getBranches(): Promise<GitBranchDto[]> {
    if (this.branchById.size === 0) {
      await this.loadBranches();
    }
    return Array.from(this.branchById.values());
  }

  async getBranchById(branchId: string): Promise<GitBranchDto | undefined> {
    if (this.branchById.size === 0) {
      await this.getBranches();
    }
    return this.branchById.get(branchId);
  }

  async getBranchTrackingStatusByIds(branchAId: string, branchBId: string): Promise<GitBranchTrackingStatusDto | undefined> {
    if (this.trackingStatusByBranchIds.size() === 0) {
      await this.loadBranchTrackingStatus();
    }
    return this.trackingStatusByBranchIds.get(branchAId, branchBId);
  }

  async getBranchTrackingStatusById(branchId: string): Promise<GitBranchTrackingStatusDto[]> {
    if (this.trackingStatusByBranchIds.size() === 0) {
      await this.loadBranchTrackingStatus();
    }
    return this.trackingStatusByBranchIds.getAllOfKey(branchId);
  }

  async getBranchesByCommitId(commitId: string): Promise<GitBranchDto[]> {
    if (!this.branchIdsByCommitId.has(commitId)) {
      await lastValueFrom(this.gitCommitService.findBranches(commitId || "")
              .pipe(tap(b =>
                      this.branchIdsByCommitId.set(b.commitId || "", b.branchIds || []))), {defaultValue: EMPTY}
      );
    }
    return (await Promise.all((this.branchIdsByCommitId.get(commitId) || []).map(b => this.getBranchById(b))))
            .flatMap(b => b ? [b] : []);
  }

  async getModifiedFilesByCommitIds(commitAId: string, commitBId: string) {
    if (!this.commitDiffFilesByCommitIds.has(commitAId, commitBId)) {
      const diffFiles =
              await lastValueFrom(this.gitCommitService.findAllModifiedFiles(commitAId, commitBId))
      this.commitDiffFilesByCommitIds.set(commitAId, commitBId, diffFiles);
      return diffFiles;
    } else {
      return this.commitDiffFilesByCommitIds.get(commitAId, commitBId);
    }
  }

  async getHunksByDiffFileId(diffFileId: string) {
    if (!this.hunksByCommitDiffFileId.has(diffFileId)) {
      const hunks =
              await lastValueFrom(this.gitDiffControllerService.findDiffHunks(diffFileId));
      this.hunksByCommitDiffFileId.set(diffFileId, hunks);
      return hunks;
    } else {
      return this.hunksByCommitDiffFileId.get(diffFileId);
    }
  }

  async getCodeChangesByDiffFileId(diffFileId: string) {
    if (!this.changedCodeByCommitDiffFileId.has(diffFileId)) {
      const codeChanges =
              await lastValueFrom(this.gitDiffControllerService.findDiffCodeChanges(diffFileId));
      this.changedCodeByCommitDiffFileId.set(diffFileId, codeChanges);
      return codeChanges;
    } else {
      return this.changedCodeByCommitDiffFileId.get(diffFileId);
    }
  }

  async getLineChangesByDiffFileId(diffFileId: string) {
    if (!this.changedLinesByCommitDiffFileId.has(diffFileId)) {
      const lines =
              await lastValueFrom(this.gitDiffControllerService.findDiffLineChanges(diffFileId));
      this.changedLinesByCommitDiffFileId.set(diffFileId, lines);
      return lines;
    } else {
      return this.changedLinesByCommitDiffFileId.get(diffFileId);
    }
  }

  async getPatch(commitId: string, parentCommitId: string, contextLines: number) {
    return await lastValueFrom(this.gitDiffControllerService.getPatch(commitId, parentCommitId, contextLines));
  }

  private async loadBranches() {
    const branchPages = this.gitBranchService.findAllBranches({page: 0, size: this.BRANCH_PAGE_SIZE}).pipe(
            expand(branchesPage => {
              if (!branchesPage.last && branchesPage.number !== undefined) {
                return this.gitBranchService.findAllBranches({
                  page: branchesPage.number + 1,
                  size: this.BRANCH_PAGE_SIZE
                });
              } else {
                return EMPTY;
              }
            }),
            tap(branchesPage => {
              (branchesPage.content || []).forEach(branch => {
                if (branch.id) {
                  this.branchById.set(branch.id, branch);
                }
              });
            }));
    await lastValueFrom(branchPages);
  }

  private async loadBranchTrackingStatus(): Promise<void> {
    const branchTrackingPages = this.gitBranchService
            .getAllTrackingStatus({page: 0, size: this.BRANCH_TRACKING_PAGE_SIZE}).pipe(
                    expand(trackingPage => {
                      if (!trackingPage.last && trackingPage.number !== undefined) {
                        return this.gitBranchService.getAllTrackingStatus({
                          page: trackingPage.number + 1,
                          size: this.BRANCH_TRACKING_PAGE_SIZE
                        });
                      } else {
                        return EMPTY;
                      }
                    }),
                    tap(trackingPage => {
                      (trackingPage.content || []).forEach(tracking => {
                        if (tracking.id) {
                          this.trackingStatusByBranchIds.set(tracking.branchAId || "",
                                  tracking.branchBId || "",
                                  tracking);
                        }
                      });
                    }));
    await lastValueFrom(branchTrackingPages);
  }

  private async loadCommitsForBranches() {
    if (this.branchById.size === 0) {
      await this.getBranches();
    }
    for (const branch of this.branchById.values()) {
      if (branch.headCommitId) {
        await this.loadCommitsAndAncestors(branch.headCommitId, true);
      }
    }
  }

  private async loadCommitsAndAncestors(commitId: string, loadAll: boolean): Promise<Observable<never>> {
    if (this.commitById.has(commitId)) {
      return EMPTY;
    }
    const commits = await lastValueFrom(this.gitCommitService.findAncestors(commitId, this.COMMIT_QUERY_DEPTH),
            {defaultValue: undefined});
    const parentIds = commits?.flatMap(commit => {
      this.commitById.set(commit.id || "", commit);
      return commit.parentIds || [];
    }) || [];
    if (loadAll) {
      return lastValueFrom(of(parentIds).pipe(
              mergeMap(parentIds => parentIds),
              filter(parentId => !this.commitById.has(parentId)),
              mergeMap(parentId => this.loadCommitsAndAncestors(parentId, loadAll))
      ), {defaultValue: EMPTY})

    } else {
      return EMPTY;
    }
  }

}
