import groovy.xml.XmlSlurper
import groovy.text.StreamingTemplateEngine

import repository.basic.Repository
//______________________________________________________________________
//
xmlFile = "../../../UTLDEV-maven/executablejar/pom.xml"

def projet = new XmlSlurper().parse(xmlFile).declareNamespace([
	'' : 'http://maven.apache.org/POM/4.0.0',
])

// dependencies = projet.dependencies.'**'.findAll { it.name() == 'dependency' }
// dependencies = projet.'*'.'dependency'
// dependencies = projet.'*'.':dependency'
dependencies = projet.':dependencies'.':dependency'
//______________________________________________________________________
//
def filename = './example1/referentiel.yaml'
def i = 0

Repository.create()
//	.manageSubObjects(false)
	.addEntry([
//prj: [
	'@meta':	"meta.design.Object",
	'@class':	"model.Project",
	gId:		Repository.required(projet.groupId as String),
	aId:		Repository.required(projet.artifactId as String),
	v:			Repository.required(projet.version as String),
	deps:		dependencies.collect { dependency -> [
		'@meta':	"meta.design.Object",
		'@class':	"model.Dependency",
		gId:		Repository.required(dependency.groupId as String),
		aId:		Repository.required(dependency.artifactId as String),
		v:			Repository.required(dependency.version as String),
		// Essais...
/*
		b: [
			c: [
				d:	"2" as Double
			]
		],
		e: [
			f:	"1" as Float
		],
		g: [
			h: [
				i:	i++ as Integer
			]
		]
*/
	]}
//]
])
	.addEntry(null, null)
	.addEntry("Clé1", null)
	.addEntry("Valeur1")
	.addEntry("Valeur1")
	.addEntry("Clé2", "Valeur2")
	.addEntry("Clé2", "Valeur3")
	.addEntry("DEL", "Valeur4")
	.removeEntry("DEL")
	.addEntry(1, null)
	.addEntry(2.0d, null)
	.addEntry(3.0f, null)
	.addEntry(new Date().getTime(), null)
	.save(filename)
//
////////////////////////////////////////////////////////////////////////
//
referentiel = Repository.create().load(filename)
	.display()

println(referentiel.entries)

println()
println(
	referentiel.entries.values().findAll { it instanceof Map && it.'@class' == "model.Dependency" && it.aId == 'junit' }
	+ referentiel.entries.values().findAll { it instanceof Map && it.'@class' == "model.Dependency" && it.aId == 'jsch' }
)

projet = referentiel.entries.'#1'
//______________________________________________________________________
//
def template = '''<%
n = 0
%>
DEPENDANCES:
------------

<%
for (dep in projet.deps) {
	n++
%>${n}) ${dep.gId}:${dep.aId}:${dep.v}
<%
}
%>
'''

docSrc = new StreamingTemplateEngine().createTemplate(template).make([
	"projet" : projet,
])

print(docSrc)
//______________________________________________________________________
//
