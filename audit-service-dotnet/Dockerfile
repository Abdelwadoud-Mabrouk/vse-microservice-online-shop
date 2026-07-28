FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY audit-service-dotnet/src/AuditService/AuditService.csproj audit-service-dotnet/src/AuditService/
COPY dotnet-shared/ dotnet-shared/
RUN dotnet restore audit-service-dotnet/src/AuditService/AuditService.csproj

COPY audit-service-dotnet/src/AuditService/ audit-service-dotnet/src/AuditService/
RUN dotnet publish audit-service-dotnet/src/AuditService/AuditService.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /app/publish .
ENV ASPNETCORE_URLS=http://+:8080
EXPOSE 8080
ENTRYPOINT ["dotnet", "AuditService.dll"]
