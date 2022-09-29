package aws.openApi;

import software.amazon.awscdk.App;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        // declare , intiate and the syntesize
        // get code from external repo
        String repositoryString = app.getNode().tryGetContext("RepositoryString").toString();
        String repositoryBranch = app.getNode().tryGetContext("RepositoryBranch").toString();

        // get codestar arn
        String codestarConnectionArn = app.getNode().tryGetContext("CodestarConnectionArn").toString();

        // get aws artifact domaina dn repo
        String codeArtifactDomain = app.getNode().tryGetContext("CodeArtifactDomain").toString();
        String codeArtifactRepository = app.getNode().tryGetContext("CodeArtifactRepository").toString();

        // new ApiStack(app,"OpenAPIBlogAPIStack", "DEV", StackProps.builder().build());

        // initiate the pipeline stack
        new PipelineStack(app, "anyBankOpenApiPipeline", repositoryString, repositoryBranch, codestarConnectionArn,
                codeArtifactRepository, codeArtifactDomain);

        //  syntesize , this groups everything in cloudformation.       
        app.synth();
    }
}
