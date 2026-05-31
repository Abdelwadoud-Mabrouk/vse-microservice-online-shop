namespace InvoiceService.Api;

public sealed class CorrelationIdMiddleware(RequestDelegate next)
{
    public async Task InvokeAsync(HttpContext context)
    {
        var correlationId = context.Request.Headers.TryGetValue("X-Correlation-Id", out var header)
            && Guid.TryParse(header.FirstOrDefault(), out var parsed)
            ? parsed
            : Guid.NewGuid();

        context.Items["CorrelationId"] = correlationId;
        context.Response.Headers["X-Correlation-Id"] = correlationId.ToString();

        await next(context);
    }
}

public static class HttpContextExtensions
{
    public static Guid GetCorrelationId(this HttpContext context, Guid fallback)
    {
        if (fallback != Guid.Empty)
        {
            context.Response.Headers["X-Correlation-Id"] = fallback.ToString();
            return fallback;
        }

        if (context.Items.TryGetValue("CorrelationId", out var value) && value is Guid correlationId)
        {
            return correlationId;
        }

        return Guid.NewGuid();
    }
}
