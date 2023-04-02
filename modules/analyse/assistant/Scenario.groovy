////////////////////////////////////////////////////////////////////////
//
//	REPOSITORY MODULE: analyse.assistant.Scenario
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
	
	expandScenarioFromParagraphs(key, value)
}
//
////////////////////////////////////////////////////////////////////////
//
private def expandScenarioFromParagraphs(key, value) {

	/*
	 * TODO:
	 * 	- Utiliser des Interfaces ?...
	 * 	- Utiliser Cucumber ?...
	 * 
	 * FIXME:
	 * 	- ins & outs sont vides tant que l'index n'est pas construit...
	 * 	- ins ramène le scénario complet au lieu de l'interaction "saisit a et b"...
	 */
	this.repository.buildIndex()
	
	def ins		= this.repository.getElementsOwning("in")
	def outs	= this.repository.getElementsOwning("out")

	return //________________________
	
	this.repository.displayIndex(key)

	println(ins)
	println()
	println(outs)
	println()
}
