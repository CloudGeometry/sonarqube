---
title: .NET Test Coverage
url: /analysis/test-coverage/dotnet-test-coverage/
---

SonarQube supports the reporting of test coverage information as part of the analysis of your .NET project.

However, SonarQube does not compile the coverage report itself.
Instead, you must set up a third-party tool to produce the coverage report as part of your build process.
During the analysis, the scanner (in this case, the [SonarScanner for .NET](/analysis/scan/sonarscanner-for-msbuild/)) picks up this report and sends it to SonarQube.
The coverage is then displayed on your SonarQube project dashboard, along with the other analysis metrics.

SonarQube supports the following .NET test coverage tools:

* Visual Studio Code Coverage
* dotnet-coverage Code Coverage
* dotCover
* OpenCover
* Coverlet

Additionally, a generic coverage format is also supported if you wish to use an unsupported tool
(though you will have to convert its output to the generic format yourself).
In this section, we discuss the directly supported tools.
For information on the generic format, see [Generic Test Data](/analysis/test-coverage/generic-test/).


## Adding coverage to your build process

The .NET scanner comes in four variants depending on which version of .NET and which CI you are using
(_.NET Framework_, _.NET Core_, _.NET tool_ and _SonarScanner for Azure DevOps_).
The setup is slightly different for each variant
(see the [SonarScanner for .NET](/analysis/scan/sonarscanner-for-msbuild/)
and [SonarScanner for Azure DevOps](/analysis/scan/sonarscanner-for-azure-devops/)
sections for details),
but the essential steps are the same.

The analysis is always split into two parts in your build process;
the begin step and the end step.
In between, you perform the actual build and your tests.
To enable coverage reporting, you need to make the following changes:

* In the scanner begin step, add the appropriate parameter to specify the location of the coverage report file that will be produced.
* Just after the build step but before the scanner end step, ensure that your test step produces the coverage report file.


## Examples using the .NET tool scanner variant

The SonarScanner for .NET comes in four major variants: .NET Framework 4.6, .NET Core, .NET tool, and Azure Pipelines extension.


### Visual Studio Code Coverage

We only recommend the use of this tool when the build agent has Visual Studio Enterprise installed or when you are using an Azure DevOps Windows image for your build.
In these cases, the .NET Framework 4.6 scanner will automatically find the coverage output generated by the `--collect "Code Coverage"` parameter without the need for an explicit report path setting.
It will also automatically convert the generated report to XML.
No further configuration is required.
Here is an example:

```
SonarScanner.MSBuild.exe begin /k:"<sonar-project-key>" /d:sonar.login="<sonar-token>" 
dotnet build --no-incremental
dotnet test --collect "Code Coverage"
SonarScanner.MSBuild.exe end /d:sonar.login="<sonar-token>"
```


### dotnet-coverage

This is a modern alternative to the Visual Studio Code Coverage provided by Microsoft (see above) that outputs results in the same format,
is cross-platform and not dependent on having Visual Studio installed.
It requires .NET Core 3.1 or later.

To use [dotnet-coverage](https://docs.microsoft.com/en-us/dotnet/core/additional-tools/dotnet-coverage),
you must install it as a global dotnet tool:

```
dotnet tool install --global dotnet-coverage
```

Using this tool, your build script would look like something like this:

```
dotnet sonarscanner begin /k:"<sonar-project-key>"
    /d:sonar.login="<sonar-token>"
    /d:sonar.cs.vscoveragexml.reportsPaths=coverage.xml
dotnet build --no-incremental
dotnet-coverage collect 'dotnet test' -f xml  -o 'coverage.xml'
dotnet sonarscanner end /d:sonar.login="<sonar-token>"
```

Note that we specify the path to the reports using `sonar.cs.vscoveragexml.reportsPaths`
because this tool’s output format is the same as the Visual Studio Code Coverage tool.
We use the `-f xml` parameter to specify that the output format is in XML.


### **dotCover**

To use [dotCover](https://www.jetbrains.com/help/dotcover/dotCover__Coverage_Analysis_on_Third-Party_Server.html)
you must install it as a global dotnet tool:

```
dotnet tool install --global JetBrains.dotCover.GlobalTool
```

Using this tool, your build script would look like something like this:

```
dotnet sonarscanner begin /k:"<sonar-project-key>"
    /d:sonar.login="<sonar-token>"
    /d:sonar.cs.dotcover.reportsPaths=dotCover.Output.html
dotnet build –no-incremental
dotnet dotcover test --dcReportType=HTML
dotnet sonarscanner end /d:sonar.login="<sonar-token>"
```

Note that we specify the path to the reports using `sonar.cs.dotcover.reportsPaths` because we are using dotCover.


### OpenCover

To use [OpenCover](https://github.com/OpenCover/opencover/wiki/Usage) you must download it from [here](https://github.com/OpenCover/opencover/releases) and unzip it in an appropriate directory, for example: `C:\tools\opencover`

Using this tool, your build script would look like something like this:

```
dotnet sonarscanner begin /k:"<sonar-project-key>"
    /d:sonar.login="<sonar-token>"
    /d:sonar.cs.opencover.reportsPaths=coverage.xml
dotnet build --no-incremental
& C:\tools\opencover\OpenCover.Console.exe -target:"dotnet.exe" 
    -targetargs:"test --no-build"
    -returntargetcode
    -output:coverage.xml
    -register:user
dotnet sonarscanner end /d:sonar.login="<sonar-token>"
```

Note that we specify the path to the reports using `sonar.cs.opencover.reportsPaths` because we are using OpenCover.


### **Coverlet**

To use Coverlet, you must install it as a global dotnet tool:

```
dotnet tool install --global coverlet.console
```

You also have to install [the coverlet collector NuGet package](https://www.nuget.org/packages/coverlet.collector/) on your test project.

Using this tool, your build script would look like something like this:

```
dotnet sonarscanner begin /k:"<sonar-project-key>"
    /d:sonar.login="<sonar-token>"
    /d:sonar.cs.opencover.reportsPaths=coverage.xml
dotnet build --no-incremental
coverlet .\CovExample.Tests\bin\Debug\net6.0\CovExample.Tests.dll
    --target "dotnet" 
    --targetargs "test --no-build"
    -f=opencover 
    -o="coverage.xml"
dotnet sonarscanner end /d:sonar.login="<sonar-token>"
```

Note that we specify the path to the reports in `sonar.cs.opencover.reportsPaths` because Coverlet produces output in the same format as OpenCover.


## .NET Framework and .NET Core scanners

In most of the examples above, we use the .NET tool scanner variant.
If you use the .NET Framework or .NET Core scanner, the commands will be a bit different but the pattern will be the same.
See [SonarScanner for .NET](/analysis/scan/sonarscanner-for-msbuild/) for details.


## SonarScanner for Azure DevOps

Using the SonarScanner for Azure DevOps and Visual Studio Code Coverage with a C# project,  your `azure-pipelines.yml` would look something like the example below.

Note that with the SonarScanner for Azure DevOps extension, the scanner `begin` step is handled by the `SonarQubePrepare` task and the scanner `end` step is handled by the `SonarQubeAnalyze` task.

Also note that because our build is running on Windows (we specify `vmImage: windows-latest`), we do not need to explicitly specify the path to the coverage report (there is no `sonar.cs.vscoveragexml.reportsPaths=coverage.xml`) nor do you need to run `codecoverage.exe` to convert the report to XML.

```
azure-pipelines.yml
trigger:
- master

variables:
- name: system.debug
  value: true 

pool:
  vmImage: windows-latest

steps:
- task: DotNetCoreCLI@2
  inputs:
    command: 'restore'
    projects: 'my-project.sln'
    feedsToUse: 'select'

- task: SonarQubePrepare@1
  inputs:
    SonarQube: 'SonarQube'
    scannerMode: 'MSBuild'
    projectKey: 'my-project-key'
    projectName: 'my-project'

- task: DotNetCoreCLI@2
  inputs:
    command: 'build'
    projects: 'my-project.sln'

- task: DotNetCoreCLI@2
  inputs:
    command: 'test'
    projects: 'tests/**/*.csproj'
    arguments: '--collect "Code Coverage"' # This is all you need to add!

- task: SonarQubeAnalyze@1
```

## **VB.NET**

The examples above are all for C# projects. For VB.NET projects the setup is identical except that you would use these parameters:

* `sonar.vbnet.vscoveragexml.reportsPaths` for Visual Studio Code Coverage
* `sonar.vbnet.dotcover.reportsPaths` for dotCover
* `sonar.vbnet.opencover.reportsPaths` for OpenCover or Coverlet

The parameter `sonar.cs.ncover3.reportsPaths` was formerly used for or NCover3.
This parameter has been deprecated.

## See Also

[Test Coverage Parameters](/analysis/test-coverage/test-coverage-parameters/).