using AuditService.Infrastructure.Persistence.Entities;
using Microsoft.EntityFrameworkCore;

namespace AuditService.Infrastructure.Persistence;

public sealed class AuditDbContext(DbContextOptions<AuditDbContext> options) : DbContext(options)
{
    public DbSet<AuditSnapshotEntity> AuditSnapshots => Set<AuditSnapshotEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(AuditDbContext).Assembly);
    }
}
