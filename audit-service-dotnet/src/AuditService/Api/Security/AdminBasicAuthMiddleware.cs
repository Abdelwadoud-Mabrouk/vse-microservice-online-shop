using System.Net;
using System.Text;

namespace AuditService.Api.Security;

public sealed class AdminBasicAuthMiddleware(
    RequestDelegate next,
    IConfiguration configuration,
    ILogger<AdminBasicAuthMiddleware> logger)
{
    private const string Realm = "Audit Admin";

    public async Task InvokeAsync(HttpContext context)
    {
        if (!IsAdminRequest(context.Request.Path))
        {
            await next(context);
            return;
        }

        if (IsAuthorized(context.Request.Headers.Authorization))
        {
            await next(context);
            return;
        }

        context.Response.Headers.WWWAuthenticate = $"Basic realm=\"{Realm}\", charset=\"UTF-8\"";
        context.Response.StatusCode = StatusCodes.Status401Unauthorized;
        await context.Response.WriteAsync("Admin login required.");
    }

    private static bool IsAdminRequest(PathString path)
    {
        return path.StartsWithSegments("/admin", StringComparison.OrdinalIgnoreCase);
    }

    private bool IsAuthorized(string? authorizationHeader)
    {
        if (string.IsNullOrWhiteSpace(authorizationHeader)
            || !authorizationHeader.StartsWith("Basic ", StringComparison.OrdinalIgnoreCase))
        {
            return false;
        }

        var expectedUser = configuration["ADMIN_USERNAME"];
        var expectedPassword = configuration["ADMIN_PASSWORD"];

        if (string.IsNullOrWhiteSpace(expectedUser) || string.IsNullOrWhiteSpace(expectedPassword))
        {
            logger.LogWarning(
                "Admin authentication is not configured. Set ADMIN_USERNAME and ADMIN_PASSWORD before using the admin area.");
            return false;
        }

        try
        {
            var encodedCredentials = authorizationHeader["Basic ".Length..].Trim();
            var credentials = Encoding.UTF8.GetString(Convert.FromBase64String(encodedCredentials));
            var separator = credentials.IndexOf(':', StringComparison.Ordinal);
            if (separator <= 0)
            {
                return false;
            }

            var username = WebUtility.UrlDecode(credentials[..separator]);
            var password = WebUtility.UrlDecode(credentials[(separator + 1)..]);

            return FixedTimeEquals(username, expectedUser)
                && FixedTimeEquals(password, expectedPassword);
        }
        catch
        {
            return false;
        }
    }

    private static bool FixedTimeEquals(string left, string right)
    {
        var leftBytes = Encoding.UTF8.GetBytes(left);
        var rightBytes = Encoding.UTF8.GetBytes(right);
        return leftBytes.Length == rightBytes.Length
            && System.Security.Cryptography.CryptographicOperations.FixedTimeEquals(leftBytes, rightBytes);
    }
}
