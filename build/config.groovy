
withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
        classNode.putNodeMetaData('projectVersion', '3.0.0')
        classNode.putNodeMetaData('projectName', 'cookiesession')
        classNode.putNodeMetaData('isPlugin', 'true')
    }
}
