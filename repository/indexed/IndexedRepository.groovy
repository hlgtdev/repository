package repository.indexed

import repository.basic.Repository
//______________________________________________________________________
//
class IndexedElement {
	
	def path
	def element

	IndexedElement(path, element) {
		
		this.path		= path
		this.element	= element
	}

	String toString() {
		
		return "${this.class}{path=${this.path}, element=${this.element}}"
	}
}
//______________________________________________________________________
//
class IndexedRepository extends Repository {

	def UNDEFINED = "[UNDEFINED-${new Date().getTime()}]"

	def index
	
	protected IndexedRepository() {

		super()
		
		index = [ : ]
		areSubObjectsManaged = false
	}
	
	def static create() {
		
		return new IndexedRepository()
	}

	def save(filename) {

		this.buildIndex()
		
		return super.save(filename)
	}

	def display() {

		this.buildIndex()
		
		return super.display()
	}

	def displayIndex(currentPath = '.') {
		
		this.buildIndex()

		def lg		= 160
		def lines	= []

		this.index.each { path, type ->
			if (path.startsWith(currentPath)) {
				def value = ''
				
				if (! type.contains(' *=')) {	// Collection
					def indexedElement = this.getElementByPath(path)
					value = "=${indexedElement == null ? indexedElement : indexedElement.element}"
				}

				def displayedPath = path.replace(currentPath, '.')
				lines << "${displayedPath} -> ${type}${value}"
			}
		}
		println('=' * lg)
		println("  INDEX CONTEXT: ${currentPath}")
		println('=' * lg)
		println(lines.join("\n"))
		println('=' * lg)
		println()
		
		return this
	}

	def getObjects() {

		return this.getElementsOwning('/@meta', META_VALUE_OBJECT)
	}

	def getElementsOwning(selector, value = null) {

		def elements = []

		this.index.findAll { path, unused ->
				path.endsWith(selector) && (value == null || getElementByPath(path).element == value)				
			}
			.findResults { path, unused -> return path == '.' ? null : path.substring(0, path.indexOf(selector)) }
			.unique()
			.each { path -> elements.add(getElementByPath(path)) }
		
		return elements
	}

	def getElementByPath(path, rootElement = UNDEFINED) {

		def element = rootElement == UNDEFINED ? this.entries : rootElement
		def previousKey = null

		for (key in path.split('/')) {
			if (! [ '', '.' ].contains(key)) {
				if (key.isInteger() && previousKey != '.') {
					key = Integer.parseInt(key)
				}
				
				try {
					element = element[key]
				}
				catch (Exception e) {
					throw new RuntimeException("### ELEMENT [${key}] NON TROUVE: path=${path}", e)
				}
			}
			previousKey = key
		}
			
		return element == null ? element : new IndexedElement(path, element)
	}

	def getParentPath(path, count = 1) {

		def liste = path.split('/') as List
		def lastIndex = liste.size() - count

		if (lastIndex < 0) {
			return null
		}
		liste = liste.subList(0, lastIndex)

		if (liste.size() == 0) {
			return null
		}
		
		return liste.join('/')
	}

	def getParentElement(path, count = 1) {

		def parentPath = this.getParentPath(path, count)

		return this.getElementByPath(parentPath)
	}
	
	def getParentObject(path, count = 1) {

		def parentPath
		def n = 0
		
		while (n < count) {
			parentPath = this.getParentPath(path)
			
			if (parentPath == null) {
				return null
			}			
			def paths = this.index.findAll { k, v ->
				def metaPath = "${parentPath}/@meta"
				k.endsWith(path) && getElementByPath(metaPath).element == META_VALUE_OBJECT
			}

			if (paths.size() > 0) {
				n++
			}
			path = parentPath
		}

		return this.getElementByPath(path)
	}

	def getParentObjectByClass(path, name) {
		
		def parentObj
		
		do {
			if (parentObj != null) {
				path = parentObj.path
			}
			parentObj = this.getParentObject(path)			
		} while (parentObj != null && parentObj.element['@class'] != name)
		
		return parentObj
	}
	
	def toMap() {

		return [
			'@meta':		META_VALUE_OBJECT,
			'@class':		"model.IndexedRepository",
			keysSequence:	this.keysSequence,	
			entries:		this.entries,
			index:			this.index,
		]
	}

	protected def buildIndex() {
		
		this.index.clear()
		
		this.buildIndex(
			this.index,
			this.entries
		)
				
		return this
	}

	protected def buildIndex(index, element, currentPath = '.') {

		if (element instanceof List) {
			def type = element.getClass().superclass.interfaces[0].getSimpleName()
			index[currentPath] = "${type} *=${element.size()}"

			element.eachWithIndex { e, i ->
				buildIndex(index, e, "${currentPath}/${i}")
			}
		}
		else if (element instanceof Map) {
			def type = element.getClass().superclass.interfaces[0].getSimpleName()
			index[currentPath] = "${type} *=${element.size()}"

			element.each { k, v ->
				buildIndex(index, v, "${currentPath}/${k}")
			}
		}
		else {
			def type = element == null ? "(${element})" : "${element.getClass().getSimpleName()}"			
			index[currentPath] = "${type}"
		}

		return this
	}	
}
