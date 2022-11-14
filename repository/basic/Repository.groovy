package repository.basic

import groovy.yaml.YamlBuilder
import groovy.yaml.YamlSlurper
//______________________________________________________________________
//
class Repository {

	static def META_VALUE_OBJECT	= 'meta.design.Object'
	
	def keysSequence
	def entries
	def areSubObjectsManaged
	def filename
	
	protected Repository() {

		keysSequence			= 0
		entries					= [ : ]
		areSubObjectsManaged	= true
		filename				= null
	}
	
	def static create() {
		
		return new Repository()
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
				key = value instanceof Map && '@meta' in value && 'id' in value
						? "${value['@meta']}::${value['id']}"
						: "#${++this.keysSequence}"
			}
			this.entries[key] = value
			key = null	// la clé des éventuels sous-objets sera à générer
		}

		if (value instanceof Map && this.areSubObjectsManaged) {
			this.addSubObjects(key, value)
		}
		this.entries = this.entries.sort()

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
			entries:		this.entries,
		]
	}
	
	def String toString() {

		def yaml = new YamlBuilder()
		yaml(this.toMap())

		return yaml.toString()
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
}
