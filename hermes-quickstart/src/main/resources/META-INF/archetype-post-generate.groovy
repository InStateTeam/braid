def deployDirectory = new File(request.outputDirectory, request.getArtifactId() + "/deploy")
deployDirectory.eachFile { file ->
    file.setExecutable(true, true)
}