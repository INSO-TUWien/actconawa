package at.ac.tuwien.inso.actconawa.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "commit_relationship")
public class GitCommitRelationship implements Serializable {

    @EmbeddedId
    private GitCommitRelationshipKey id;

    @ManyToOne
    @MapsId("parent_id")
    private GitCommit parent;

    @ManyToOne
    @MapsId("commit_id")
    private GitCommit child;

    public GitCommitRelationshipKey getId() {
        return id;
    }

    public void setId(GitCommitRelationshipKey id) {
        this.id = id;
    }

    public GitCommit getParent() {
        return parent;
    }

    public void setParent(GitCommit parent) {
        this.parent = parent;
    }

    public GitCommit getChild() {
        return child;
    }

    public void setChild(GitCommit child) {
        this.child = child;
    }
}
