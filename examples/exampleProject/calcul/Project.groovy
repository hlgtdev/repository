import repository.indexed.IndexedRepository
//______________________________________________________________________
//
def LITERATE	= "meta.file.Literate"
def projectDir	= args.size() == 0 ? '.' : args[0]
//______________________________________________________________________
//
this.referentiel = IndexedRepository.create()
	.use(LITERATE)
	.addMetaFiles("$projectDir/input", ".lit", LITERATE)
//______________________________________________________________________
//
	.save("$projectDir/work/project-repository.yaml")
	.displayIndex()
//______________________________________________________________________
//
