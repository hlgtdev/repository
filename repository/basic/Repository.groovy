package repository.basic

import groovy.yaml.YamlBuilder
import groovy.yaml.YamlSlurper
//______________________________________________________________________
//
class Repository {

	static def META_VALUE_OBJECT	= 'meta.design.Object'
	
	static def MODULES_PATH			= "/home/jl/projets/UTLDEV-repository/groovy/modules/"
	static def GROOVY_SHELL			= new GroovyShell()
	
	def keysSequence
	def entries
	def areSubObjectsManaged
	def filename
	def modules
	def lastEntry
	
	protected Repository() {

		keysSequence			= 0
		entries					= [ : ]
		areSubObjectsManaged	= true
		filename				= null
		modules					= [ : ]
	}
	
	def static create() {
		
		return new Repository()
	}
	
	def use(module) {
		
		if (! (module in this.modules)) {
			def moduleFile = new File(MODULES_PATH + module.replaceAll("\\.", "/") + ".groovy")
			
			if (! moduleFile.exists()) {
				throw new RuntimeException("### MODULE NON TROUVE: $module\n")
			}
			def moduleScript = GROOVY_SHELL.parse(moduleFile)				
			moduleScript.initModule(this)

			this.modules[module] = moduleScript
		}
		
		return this
	}
	
	def static required(value) {
		
		assert value != null, "VALEUR OBLIGATOIRE"
		
		if (value instanceof String) {
			assert value.trim() != "", "VALEUR OBLIGATOIRE"
		}
		return value
	}

	def manageSubObjects(bool) {

		this.areSubObjectsManaged = bool
		
		return this
	}

	def addEntry(value) {

		addEntry(null, value)

		return this
	}

	def addEntry(key, value) {
			
		key = key == null || key instanceof String ? key : key as String

		if (! (value instanceof Map) || ! ('@meta' in value) || ! this.areSubObjectsManaged) {
			if (key == null) {
				def idPrefix = value instanceof Map && '@meta' in value ? "${value['@meta']}::" : ""
				key = value instanceof Map && 'id' in value ? "${idPrefix}${value['id']}" : "#${++this.keysSequence}"
			}

			moduleDelegate("onValidate", key, value)
			this.entries[key] = value
			this.lastEntry = key
			moduleDelegate("onEntryAdded", key, value)

			key = null	// la clé des éventuels sous-objets sera à générer
		}

		if (value instanceof Map && this.areSubObjectsManaged) {
			this.addSubObjects(key, value)
		}
		this.entries = this.entries.sort()
		
		return this
	}

	def addEntryFromYamlFile(yamlFile) {

		this.addEntryFromYamlFile(null, yamlFile)

		return this
	}

	def addEntryFromYamlFile(key, yamlFile) {

		def yamlSrc = yamlFile.text
		
		this.addEntryFromYaml(key, yamlSrc)

		return this
	}

	def addEntryFromYaml(yamlSrc) {

		this.addEntryFromYaml(null, yamlSrc)

		return this
	}

	def addEntryFromYaml(key, yamlSrc) {

		def value = this.createValueFromYaml(yamlSrc)

		this.addEntry(key, value)

		return this
	}

	def createValueFromYaml(yamlSrc) {

		def value = new YamlSlurper().parseText(formatYaml(yamlSrc))

		return value
	}

	def formatYaml(yamlSrc) {

		yamlSrc = yamlSrc.replace('\t', '  '
			).replace('-  ', '- '
			)

		return yamlSrc
	}

	def removeEntry(key) {

		key = key == null || key instanceof String ? key : key as String

		def hasChildren = entries.values().findAll { it instanceof Map && it.'@parent' == key }.size() > 0
		assert ! hasChildren, "Suppression de l'entrée [$key] impossible: ELLE POSSEDE DES ENFANTS"

		this.entries.remove(key)

		return this
	}

	def load(filename) {

		this.filename = filename
		def map	= new YamlSlurper().parseText(new File(this.filename).text)
		
		this.toMap().keySet().each { k ->
			if (! k.startsWith('@')) {
				this.(k) = map.(k)
			}
		}
		
		return this
	}

	def save(filename = null) {
		
		if (filename != null) {
			this.filename = filename
		}
		assert this.filename != null, "### LE NOM DE FICHIER DOIT ETRE FOURNI"
		
		new File(this.filename).write(this.toString())

		return this
	}

	def display() {

		println(this.toString())
		
		return this
	}

	def toMap() {

		return [
			'@meta':		META_VALUE_OBJECT,
			'@class':		"model.Repository",
			keysSequence:	this.keysSequence,	
			modules:		this.modules.keySet(),
			entries:		this.entries,
		]
	}
	
	def String toString() {

		def yaml = new YamlBuilder()
		yaml(this.toMap())

		return yaml.toString()
	}
	
	def addMetaFile(filename, meta) {
				
		def id = filename.replaceAll("/", "\\\\")
		
		this.addEntryFromYaml("""
			'@meta'		: ${meta}
			id			: ${id}
			filename	: ${filename}
		""")
		
		return this
	}

	def addMetaFiles(rootDir, extension, meta) {

		println("-" * 160)

		def n = 0
		
		new File(rootDir).eachFileRecurse { f ->
			
			if (f.name.endsWith(extension)) {
				this.addMetaFile(f.toString(), meta)
				n++
			}
		}
		println("-" * 160)
		println(">>> [ # FILES] $n")
		println("-" * 160)

		return this
	}
	
	private def addSubObjects(key, root, parentId = null) {

		if (root instanceof List) {
			return root.collect {
				if (it instanceof Map || it instanceof List) {
					it = addSubObjects(key = null, it, parentId)

					if (it instanceof Map && '@meta' in it && it.'@meta' == META_VALUE_OBJECT) {
						it.'@parent' = parentId
					}
				}
				return it
			}
		} else if (root instanceof Map) {
			if ('@meta' in root && root.'@meta' == META_VALUE_OBJECT) {
				parentId = root.'@id' = key == null ? "#${++this.keysSequence}" : key
			}
			root = root.collectEntries {k,v ->
				if (v instanceof Map || v instanceof List) {
					v = addSubObjects(key = null, v, parentId)

					if (v instanceof Map && '@meta' in v && v.'@meta' == META_VALUE_OBJECT) {
						v.'@parent' = root.'@id'
					}
				}
				return [ (k) : v ]
			}.sort()

			if ('@meta' in root && root.'@meta' == META_VALUE_OBJECT) {
				this.entries[root.'@id'] = root
			}
			return root
		} else {
			return root
		}
	}
	
	private def moduleDelegate(method, key, value) {
		
		if (value instanceof Map && '@meta' in value) {
			def meta = value.'@meta'
			
			if (meta in modules) {
				modules[meta]."${method}"(key, value)
			}
		}
	}
}
