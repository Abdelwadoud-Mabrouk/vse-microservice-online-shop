namespace WarehouseService.Api.Endpoints;

public static class SystemEndpoints
{
    public static IEndpointRouteBuilder MapSystemEndpoints(this IEndpointRouteBuilder app)
    {
        app.MapGet("/health", () => Results.Ok(new { service = "warehouse-service", status = "healthy" }));
        app.MapGet("/", () => Results.Redirect("/swagger"));

        return app;
    }
}
