import groovy.xml.XmlSlurper
import groovy.text.StreamingTemplateEngine

import repository.basic.Repository
import repository.indexed.IndexedRepository
//______________________________________________________________________
//
def filename = './exampleCalcul/referentiel-indexe.yaml'
def processOrder = 0

def referentiel = IndexedRepository.create()
	.use("analyse.assistant.Scenario")
//______________________________________________________________________
//
	.addEntry(ref='GEN_TARGET', value='python')
//______________________________________________________________________
//
	.addEntry(ref='entier::a', value=1)
	.addEntry(ref='entier::b', value=2)
	.addEntry(ref='entier::r', value=3)
//______________________________________________________________________
//
	.addEntryFromYaml("""
		id: Additionner
		'@meta': analyse.assistant.Scenario
		participants:
			u:
				§: Utilisateur
			s:
				§: Système
		interactions:
		-	'u>s':
				§:	saisit a et b
				in:
					a: |-
						{{REF=entier::a}}
					b: |-
						{{REF=entier::b}}
		-	'u>s':
				§:	clique sur +
		-	'@s':
				id:	addAtoB
				§:	additionne a et b
		-	'u<s':
				§:	affiche r
				out:
					r: |-
						{{REF=entier::r}}
	""")
//______________________________________________________________________
//
	.addEntryFromYaml("""
		id: Additionner
		'@meta':	conception.assistant.Scenario
		extend:		analyse.assistant.Scenario::Additionner
		participants:
			calcEP:
				rest:
					path : /calcul
				class: application.endpoint.CalculEndPoint
			calcS:
				class: application.service.CalculService
		after:
			addAtoB:
				impl:
				-	New:
						participant: calcS
						§: |-
							this.calculService = {{GTG=constructor}}()
				-	New:
						participant: calcEP
						§: |-
							this.calculEndPoint = {{GTG=constructor}}(
								calculService: application.service.CalculService = calculService)
				-	Call:
						participant: calcEP
						rest:
							method : GET
							path : /addition?a={a}&b={b}
						§: |-
							r = this.calculEndpoint.additionner(
								a: int = {{REF=entier::a}},
								b: int = {{REF=entier::b}}): int
						impl:
						- 	If:
								§: |-
									non {{GTG=paramètres valides: a, b}}
								then:
								-	Raise:
										§: |-
											ServiceException('Paramètres invalides: a=%s b=%s': a, b)
						-	Set:
								§: |-
									bUnchanged: int = 0
						-	Set:
								§: |-
									bUnchanged = b + 1 - 1
						-	Call:
								participant: calcS
								§: |-
									r = this.calculService.additionner(
										a: int = a,
										b: int = bUnchanged + 0): int
								impl:
								-	Return: a + b
						-	Return: r
	""")
//______________________________________________________________________
//
//	TODO: Décorer les input models à partir des §...
//______________________________________________________________________
//
	.addEntryFromYaml("""
		'@meta': Query
		id: analyse participants
		find:
		-	participants
		where:
		-	'@meta':
			-	analyse.Scenario
	""")
//______________________________________________________________________
//
	.addEntryFromYaml("""
		'@meta': Processor
		id: plantuml actors
		processOrder:	${++processOrder}
		source:			Query::analyse participants
		process:
		-	def:
			-	s:	""
			-	n:	0
		-	add:
			- 	entry:
					key:	participantId
					value:	|-
						'______________________________________________________________________
						'
						'	DEBUT PARTICIPANTS
						'______________________________________________________________________
						'
		-	forEach:
				item:	participant
				process:
				-	def:
					-	participantId:		participant.key
					-	participantLabel:	participant.key.§
				-	add:
					- 	entry:
							key:	participantId
							concat:
							-	"actor "
							-	participantId
							-	": "
							-	participantLabel
		-	add:
			- 	entry:
					key:	participantId
					value:	|-
						'______________________________________________________________________
						'
						'	FIN PARTICIPANTS
						'______________________________________________________________________
						'
	""")
//______________________________________________________________________
//
	.save(filename)
//	.displayIndex()
//______________________________________________________________________
//
/* TODO:
 * 		- Gérer l'exécution d'une Query
 */
// println(referentiel.getValuedIndex())
