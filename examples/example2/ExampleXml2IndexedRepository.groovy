import groovy.xml.XmlSlurper
import groovy.text.StreamingTemplateEngine

import repository.basic.Repository
import repository.indexed.IndexedRepository
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
def filename = './example2/referentiel-indexe.yaml'
def i = 0

IndexedRepository.create()
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
referentiel = IndexedRepository.create().load(filename)
	.display()
	.displayIndex()
	.displayIndex('./#1/deps')

println(referentiel.entries)
println()

println(referentiel.index)
println()

println(referentiel.getElementByPath('./#1/UNKNOWN'))
println()

println(referentiel.getElementsOwning('/deps'))
println()

println(referentiel.getElementsOwning('/gId').findAll { it.element.gId == 'org.codehaus.jackson' })
println(referentiel.getElementsOwning('/gId', 'org.codehaus.jackson'))
println()
	
refObjects = referentiel.getObjects()

println(
	refObjects.findAll { it.element.'@class' == "model.Dependency" && it.element.aId == 'junit' }
	+ refObjects.findAll { it.element.'@class' == "model.Dependency" && it.element.aId == 'jsch' }
)
println()

path = refObjects[3].path

println(path)
println()

println(referentiel.getParentPath(path))
println()

println(referentiel.getParentElement(path))
println()

println(referentiel.getParentObjectByClass(path, "model.Project"))
println()

projet = referentiel.getParentObject(path).element

println(projet)
println()
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
referentiel.addEntryFromYaml("essai.yaml::model.Project", """
	'@meta':	meta.design.Class
	id:			model.Project
	name:		Project
	package:	model
	extends:	null
	abstract:	false

	attributes:
	-	id:			'gId: java.lang.String'
		name:		gId
		type:		java.lang.String

	-	id:			'aId: java.lang.String'
		name:		aId
		type:		java.lang.String

	-	id:			'v: java.lang.String'
		name:		v
		type:		java.lang.String

	relations:
	-	id:			'deps: java.util.List<model.Dependency>'
		name:		deps
		type:		java.util.List
		subType:	model.Dependency

	methods:		[]
""")

referentiel.save()
//______________________________________________________________________
//
