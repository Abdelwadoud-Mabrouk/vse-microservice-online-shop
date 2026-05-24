using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace WarehouseService.Infrastructure.Persistence;

public sealed class WarehouseDbContextFactory : IDesignTimeDbContextFactory<WarehouseDbContext>
{
    public WarehouseDbContext CreateDbContext(string[] args)
    {
        var connectionString = Environment.GetEnvironmentVariable("WAREHOUSE_DB_CONNECTION")
            ?? throw new InvalidOperationException("Warehouse database connection is missing. Set WAREHOUSE_DB_CONNECTION before running EF commands.");

        var options = new DbContextOptionsBuilder<WarehouseDbContext>()
            .UseNpgsql(connectionString)
            .Options;

        return new WarehouseDbContext(options);
    }
}
