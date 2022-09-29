package aws.openApi;

import java.util.Arrays;
import java.util.HashMap;

import software.constructs.Construct;
import software.amazon.awscdk.Arn;
import software.amazon.awscdk.ArnComponents;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.pipelines.AddStageOpts;
import software.amazon.awscdk.pipelines.CodeBuildStep;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.ConnectionSourceOptions;
import software.amazon.awscdk.pipelines.ShellStep;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;

public class PipelineStack extends Stack {

	// public PipelineStack(final Construct scope, final String id,
	// 		String repositoryPath, String repositoryBranch, String connectionArn,
	// 		String codeArtifactRepositoryName, String codeArtifactDomainName) {

	// 	this(scope, id, repositoryPath, repositoryBranch, connectionArn,
	// 			codeArtifactRepositoryName, codeArtifactDomainName, null);
	// }

	public PipelineStack(final Construct scope, final String id,
			String repositoryPath, String repositoryBrach, String connectionArn,
			String codeArtifactRepositoryName, String codeArtifactDomainName, final StackProps props) {

		super(scope, id, props);

		// create pipeline source
		CodePipelineSource pipelineSource = CodePipelineSource.connection(repositoryPath, repositoryBrach,
				ConnectionSourceOptions.builder()
						.connectionArn(connectionArn)
						.build());

		
		// sythesize step, take pipeline source and run cdk synth command
		ShellStep synthStep = ShellStep.Builder.create("Synth")
				.input(pipelineSource)
				.commands(Arrays.asList("npm install -g aws-cdk@2.19.0", "cd cdk", "cdk synth"))
				.primaryOutputDirectory("cdk/cdk.out")
				.build();
		
		// validate the pipeline		
		final CodePipeline pipeline = CodePipeline.Builder.create(this, "anyBankOpenApiPipeline")
				.pipelineName("anyBankOpenApiPipeline")
				.selfMutation(true)
				.dockerEnabledForSynth(true)
				.synth(synthStep)
				.build();
		
		// publish the assets created from cdk synth.
		// publish the auto generated code to code artifact 
		PolicyStatement codeArtifactStatement = PolicyStatement.Builder.create()
				.sid("CodeArtifact")
				.effect(Effect.ALLOW)
				.actions(Arrays.asList("codeartifact:GetAuthorizationToken", "codeartifact:GetRepositoryEndpoint",
						"codeartifact:ReadFromRepository", "codeartifact:DescribeRepository",
						"codeartifact:PublishPackageVersion", "codeartifact:PutPackageMetadata"))
				.resources(Arrays.asList(
						Arn.format(
								ArnComponents.builder()
										.service("codeartifact")
										.resource(String.format("repository/%s/%s", codeArtifactDomainName,
												codeArtifactRepositoryName))
										.build(),
								this),
						Arn.format(
								ArnComponents.builder()
										.service("codeartifact")
										.resource(String.format("domain/%s", codeArtifactDomainName))
										.build(),
								this),
						Arn.format(
								ArnComponents.builder()
										.service("codeartifact")
										.resource(String.format("package/%s/%s/*", codeArtifactDomainName,
												codeArtifactRepositoryName))
										.build(),
								this)))
				.build();

		// why sts ?
		PolicyStatement codeArtifactStsStatement = PolicyStatement.Builder.create()
				.sid("CodeArtifactStsStatement")
				.effect(Effect.ALLOW)
				.actions(Arrays.asList("sts:GetServiceBearerToken"))
				.resources(Arrays.asList("*"))
				.conditions(new HashMap<String, Object>() {
					{
						put("StringEquals", new HashMap<String, String>() {
							{
								put("sts:AWSServiceName", "codeartifact.amazonaws.com");
							}
						});
					}
				})
				.build();

		
		// run the auto code generator for open api		
		CodeBuildStep codeArtifactStep = CodeBuildStep.Builder.create("CodeArtifactDeploy")
				.input(pipelineSource)
				.commands(Arrays.asList(
						"echo $REPOSITORY_DOMAIN",
						"echo $REPOSITORY_NAME",
						"export CODEARTIFACT_TOKEN=`aws --region us-east-1 codeartifact get-authorization-token --domain $REPOSITORY_DOMAIN --query authorizationToken --output text`",
						"export REPOSITORY_ENDPOINT=$(aws --region us-east-1 codeartifact get-repository-endpoint --domain $REPOSITORY_DOMAIN --repository $REPOSITORY_NAME --format maven | jq .repositoryEndpoint | sed 's/\\\"//g')",
						"echo $REPOSITORY_ENDPOINT",
						"cd api",
						"wget -q https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/5.4.0/openapi-generator-cli-5.4.0.jar -O openapi-generator-cli.jar",
						"cp ./maven-settings.xml /root/.m2/settings.xml",
						"java -jar openapi-generator-cli.jar batch openapi-generator-config.yaml",
						"cd client",
						"mvn -version",
						// mvn --no-transfer-progress for mvn >= 3.6.1
						"mvn --no-transfer-progress deploy -DaltDeploymentRepository=openapi--prod::default::$REPOSITORY_ENDPOINT"))
				.rolePolicyStatements(Arrays.asList(codeArtifactStatement, codeArtifactStsStatement))
				.env(new HashMap<String, String>() {
					{
						put("REPOSITORY_DOMAIN", codeArtifactDomainName);
						put("REPOSITORY_NAME", codeArtifactRepositoryName);
					}
				})
				.build();

		ApiStackStage apiStackStageDev = new ApiStackStage(this, "DEV", StageProps.builder().build());
		pipeline.addStage(apiStackStageDev,
				AddStageOpts.builder()
						.post(Arrays.asList(codeArtifactStep))
						.build());

	}

}
