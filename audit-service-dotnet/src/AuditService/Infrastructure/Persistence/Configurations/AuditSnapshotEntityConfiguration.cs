using AuditService.Infrastructure.Persistence.Entities;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace AuditService.Infrastructure.Persistence.Configurations;

public sealed class AuditSnapshotEntityConfiguration : IEntityTypeConfiguration<AuditSnapshotEntity>
{
    public void Configure(EntityTypeBuilder<AuditSnapshotEntity> builder)
    {
        builder.ToTable("audit_snapshots");

        builder.HasKey(snapshot => snapshot.Id);

        builder.Property(snapshot => snapshot.Id).HasColumnName("id");
        builder.Property(snapshot => snapshot.CorrelationId).HasColumnName("correlation_id").IsRequired();
        builder.Property(snapshot => snapshot.EventType).HasColumnName("event_type").HasMaxLength(120).IsRequired();
        builder.Property(snapshot => snapshot.Service).HasColumnName("service").HasMaxLength(120).IsRequired();
        builder.Property(snapshot => snapshot.TimestampUtc).HasColumnName("timestamp_utc").IsRequired();
        builder.Property(snapshot => snapshot.PayloadJson).HasColumnName("payload").HasColumnType("jsonb").IsRequired();
        builder.Property(snapshot => snapshot.PreviousEventId).HasColumnName("previous_event_id");
        builder.Property(snapshot => snapshot.Actor).HasColumnName("actor").HasMaxLength(120).IsRequired();
        builder.Property(snapshot => snapshot.StatusCode).HasColumnName("status_code").HasMaxLength(40).IsRequired();

        builder.HasIndex(snapshot => new { snapshot.CorrelationId, snapshot.TimestampUtc })
            .HasDatabaseName("ix_audit_snapshots_correlation_timestamp");
    }
}
