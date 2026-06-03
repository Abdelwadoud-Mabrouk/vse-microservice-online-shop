using AuditService.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;

namespace AuditService.Infrastructure.Persistence.Migrations;

[DbContext(typeof(AuditDbContext))]
public sealed class AuditDbContextModelSnapshot : ModelSnapshot
{
    protected override void BuildModel(ModelBuilder modelBuilder)
    {
        modelBuilder
            .HasAnnotation("ProductVersion", "10.0.4");

        modelBuilder.Entity("AuditService.Infrastructure.Persistence.Entities.AuditSnapshotEntity", builder =>
        {
            builder.Property<Guid>("Id")
                .HasColumnName("id");

            builder.Property<string>("Actor")
                .IsRequired()
                .HasMaxLength(120)
                .HasColumnName("actor");

            builder.Property<Guid>("CorrelationId")
                .HasColumnName("correlation_id");

            builder.Property<string>("EventType")
                .IsRequired()
                .HasMaxLength(120)
                .HasColumnName("event_type");

            builder.Property<string>("PayloadJson")
                .IsRequired()
                .HasColumnType("jsonb")
                .HasColumnName("payload");

            builder.Property<Guid?>("PreviousEventId")
                .HasColumnName("previous_event_id");

            builder.Property<string>("Service")
                .IsRequired()
                .HasMaxLength(120)
                .HasColumnName("service");

            builder.Property<string>("StatusCode")
                .IsRequired()
                .HasMaxLength(40)
                .HasColumnName("status_code");

            builder.Property<DateTimeOffset>("TimestampUtc")
                .HasColumnName("timestamp_utc");

            builder.HasKey("Id")
                .HasName("pk_audit_snapshots");

            builder.HasIndex("CorrelationId", "TimestampUtc")
                .HasDatabaseName("ix_audit_snapshots_correlation_timestamp");

            builder.ToTable("audit_snapshots");
        });
    }
}
