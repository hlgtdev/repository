////////////////////////////////////////////////////////////////////////
//
//	REPOSITORY MODULE: meta.file.Literate
//
////////////////////////////////////////////////////////////////////////
//
def initModule(repository) {
	
	this.repository = repository
}
//______________________________________________________________________
//
def onValidate(key, value) {

//	println(">>> ${this.class}.onValidate: $key=$value")
//	println()
}
//______________________________________________________________________
//
def onEntryAdded(key, value) {

	def file = new File(value['filename'])
	
	this.processLiterateFile(file)
}
//
////////////////////////////////////////////////////////////////////////
//
def referentiel			= null
def fileKey				= null
def isFileEntryAdded	= false
def key					= null
def action				= null
def textBlock			= null
def valueBlock			= null
def isValueBlock		= false
//______________________________________________________________________
//
def processLiterateFile(file) {

	println(">>> [LITERATE] Processing ${file}...")

	referentiel = this.repository
	
	this.fileKey = file.toString().replaceAll("/", "\\\\")

	def entry = referentiel.getElementByPath("meta.file.Literate::${this.fileKey}").element
	entry["uses"]		= []
	entry["directives"]	= []

	def content = file.text

	content = content.replaceAll(/(?s)\/\*.*?\*\/\s*/, "")	// Suppression des blocs de commentaires
	content = content.replaceAll(/(?s)\/\/[^\r\n]*\s*/, "") // Suppression des lignes de commentaires

	def lines = content.split("\n")	// Récupération de la liste des lignes significatives restantes
	
	key = null
	action = null
	textBlock = ""
	valueBlock = ""
	
	for (line in lines) {
//		println(line)
		
		if (line.startsWith("<<")) {
			textBlock = manageTextBlock(textBlock)
			isFileEntryAdded = false

			manageRepositoryValue(line, ">>")
		}
		else if (line.startsWith("{{")) {
			textBlock = manageTextBlock(textBlock)
			isFileEntryAdded = false

			manageRepositoryValue(line, "}}")
		}
		else if (line.startsWith("§§")) {
			textBlock = manageTextBlock(textBlock)
			
			if (action != null) {
				valueBlock = this."$action"(valueBlock)
			}

			if (valueBlock != null) {
				referentiel.addEntry(key, valueBlock)
				
				if (! isFileEntryAdded) {
					def fileEntries = referentiel.getElementByPath("meta.file.Literate::${this.fileKey}/directives").element
					fileEntries << [
						"key"		: referentiel.lastEntry.toString(),
						"action"	: action,
						"type"		: "set"
					]
					isFileEntryAdded = true
				}
			}
			valueBlock = ""
		}
		else {
			if (isValueBlock) {
				// Ajout de la ligne au bloc de valeur
				valueBlock += "${line}\n"
			}
			else {
				// Ajout de la ligne au bloc de texte Markdown
				textBlock += "${line}\n"
			}
		}
	}
}
//______________________________________________________________________
//
def manageTextBlock(textBlock) {
	
	if (textBlock != "") {
		referentiel.addEntry(textBlock)

		def fileEntries = referentiel.getElementByPath("meta.file.Literate::${this.fileKey}/directives").element
		fileEntries <<  [
			"key"		: referentiel.lastEntry.toString(),
			"action"	: null,
			"type"		: "text"
		]
		textBlock = ""
	}

	return textBlock
}
//______________________________________________________________________
//
def manageRepositoryValue(line, endToken) {

	def p = line.indexOf(endToken)
	
	if (p != -1) {
		def directive = line.substring(2, p)
		p = directive.indexOf("@")
		key = directive
		action = ""

		if (p != -1) {
			key = directive.substring(0, p)
			action = directive.substring(p + 1)
		}

		key = key == "" ? null : key
		action = action == "" ? null : action				
		p = line.indexOf("=")

		if (key != null) {
			def fileEntries = referentiel.getElementByPath("meta.file.Literate::${this.fileKey}/directives").element
			def type = p == -1 ? "get" : "set"
			type = endToken == "}}" ? "${type}&show" : type
			fileEntries <<  [
				"key"		: key,
				"action"	: action,
				"type"		: type.toString()
			]
			isFileEntryAdded = true
		}

		if (p != -1) {
			def value = line.substring(p + 1)
			isValueBlock = value ==~ /^.?§.?$/
			
			if (isValueBlock) {
				valueBlock = ""
			}
			else {
				if (action != null) {
					value = this."$action"(value)
				}
				
				if (value != null) {
					referentiel.addEntry(key, value)

					if (! isFileEntryAdded) {
						def fileEntries = referentiel.getElementByPath("meta.file.Literate::${this.fileKey}/directives").element
						def type = p == -1 ? "get" : "set"
						type = endToken == "}}" ? "${type}&show" : type
						fileEntries <<  [
							"key"		: referentiel.lastEntry.toString(),
							"action"	: action,
							"type"		: type.toString()
						]
						isFileEntryAdded = true
					}
				}
			}
		}
	}
}
//______________________________________________________________________
//
def use(moduleId) {

	def modules = referentiel.getElementByPath("meta.file.Literate::${this.fileKey}/uses").element
	
	if (! modules.contains(moduleId)) {
		modules << moduleId

		referentiel.use(moduleId)
	}

	return null
}
//______________________________________________________________________
//
def eval(value) {

	value = Eval.me(value)

	return value
}
//______________________________________________________________________
//
def yaml(src) {
	
	def yaml = referentiel.createValueFromYaml(src)
	
	return yaml
}
//______________________________________________________________________
//
def yamlFile(fileName) {
	
	def yaml = yaml(new File(fileName).text)

	return yaml
}
//______________________________________________________________________
//
def yamlFiles(directoryAndExtension) {
	
	def p = directoryAndExtension.lastIndexOf('/')
	def directory = directoryAndExtension.substring(0, p)
	def extension = directoryAndExtension.substring(p + 1).replace("*", "")

	new File(directory).eachFileRecurse { File file ->
		
		if (file.name.endsWith(extension)) {
			referentiel.addEntry(yamlFile(file.toString()))

			def fileEntries = referentiel.getElementByPath("meta.file.Literate::${this.fileKey}/directives").element
			fileEntries <<  [
				"key"		: referentiel.lastEntry.toString(),
				"action"	: "yamlFiles",
				"type"		: "set"
			]
		}
	}

	return null
}
//______________________________________________________________________
//
