namespace AuditService.Api.Endpoints;

public static class SystemEndpoints
{
    public static IEndpointRouteBuilder MapSystemEndpoints(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapGet("/health", () => Results.Ok(new { service = "audit-service", status = "healthy" }));
        endpoints.MapGet("/", () => Results.Redirect("/swagger"));

        return endpoints;
    }
}
