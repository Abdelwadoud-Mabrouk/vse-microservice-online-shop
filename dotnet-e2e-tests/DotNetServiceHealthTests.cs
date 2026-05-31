[Collection(E2ETestCollection.Name)]
public sealed class DotNetServiceHealthTests
{
    [Fact]
    public async Task All_dotnet_services_are_reachable()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(20));
        var settings = EndToEndTestSettings.FromEnvironment();
        using var client = new DotNetServiceClient(settings);

        await client.AssertHealthyAsync(cts.Token);
    }
}
