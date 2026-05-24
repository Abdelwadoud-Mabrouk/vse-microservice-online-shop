using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AuditService.Infrastructure.Persistence.Migrations;

public partial class InitialAuditSchema : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.CreateTable(
            name: "audit_snapshots",
            columns: table => new
            {
                id = table.Column<Guid>(type: "uuid", nullable: false),
                correlation_id = table.Column<Guid>(type: "uuid", nullable: false),
                event_type = table.Column<string>(type: "character varying(120)", maxLength: 120, nullable: false),
                service = table.Column<string>(type: "character varying(120)", maxLength: 120, nullable: false),
                timestamp_utc = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                payload = table.Column<string>(type: "jsonb", nullable: false),
                previous_event_id = table.Column<Guid>(type: "uuid", nullable: true),
                actor = table.Column<string>(type: "character varying(120)", maxLength: 120, nullable: false),
                status_code = table.Column<string>(type: "character varying(40)", maxLength: 40, nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("pk_audit_snapshots", x => x.id);
            });

        migrationBuilder.CreateIndex(
            name: "ix_audit_snapshots_correlation_timestamp",
            table: "audit_snapshots",
            columns: ["correlation_id", "timestamp_utc"]);
    }

    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropTable(name: "audit_snapshots");
    }
}
