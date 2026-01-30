package com.skillstorm.finsight.documents_cases.models;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "case_note")
public class CaseNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "author_user_id", nullable = false, length = 36, columnDefinition = "VARCHAR(36)")
    private String authorUserId;

    @Column(name = "note_text", nullable = false, columnDefinition = "TEXT")
    private String noteText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public CaseNote() {
    }

    public CaseNote(Long noteId, Long caseId, String authorUserId, String noteText, Instant createdAt) {
        this.noteId = noteId;
        this.caseId = caseId;
        this.authorUserId = authorUserId;
        this.noteText = noteText;
        this.createdAt = createdAt;
    }
    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(String authorUserId) {
        this.authorUserId = authorUserId;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseNote caseNote = (CaseNote) o;
        return Objects.equals(noteId, caseNote.noteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noteId);
    }

    @Override
    public String toString() {
        return "CaseNote{" +
                "noteId=" + noteId +
                ", caseId=" + caseId +
                ", authorUserId=" + authorUserId +
                ", noteText='" + noteText + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
