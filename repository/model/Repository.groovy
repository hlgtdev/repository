package model

import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder

class Repository {

    def defaultPathsSequence = null
    def currentDefaultPath = null
    def currentPath = null
    def entries = null
    def index = null
    def yamlParser = null
    def yamlBuilder = null
    def regexpIntegerKey = null
    def modules = null
    def modulesPath = null
    def saveToFilename = null

    private Repository()
    {
      defaultPathsSequence = 0
      entries = [ : ]
      index = [ : ]
      modules = [ : ]
      yamlParser = new YamlSlurper()
      yamlBuilder = new YamlBuilder()
      regexpIntegerKey = /\((\d+)\)/
      modulesPath = "~/projets/UTLDEV-repository/refactoring/modules/"
    
      this.class.classLoader.addClasspath(modulesPath)
    }

    static def create()
    {
      // Créé un référentiel vide
      return new Repository()
    }

    def setElement(path, element)
    {
      def WIP = false    ////////////////////////////////////////
      
      def isDefaultPath = false
      
      if (path == null) {
        if (element != null && element instanceof Map && 'id' in element) {
          // Si le chemin n'est pas renseigné et que l'élément possède un id, alors le chemin est: {element.id}
          path = "${element.id}"
          
          // Si le chemin est {element.id} et que l'élément possède un @meta, alors le chemin est: {element.@meta}::{element.id}
          if ('@meta' in element) {
            path = "${element.'@meta'}::$path"
          }
          path = "./$path".toString()
        }
      }
      
      // Si le chemin n'est pas renseigné, un chemin est créé de la forme: ./#{n° de séquence}
      if (path == null) {
        path = "./#${++this.defaultPathsSequence}".toString()
        isDefaultPath = true
      }
    
      // Si le chemin n'est pas une chaine de caractères, un chemin est créé de la forme: ./{path.toString()}
      if (! (path instanceof String)) {
        path = "./${path.toString()}".toString()
      }
      
      def keys = path.split("/")
      def keysCount = keys.size()
      def currentHolder = this.entries
      def currentKey = null
      def subPath = "."
    
      for (def i = 0; true; i++) {
        currentKey = keys[i]
        
        // On ignore la pseudo-clé: courante
        if (currentKey == ".") {
          continue
        }
        
        // Si la clé à la forme ({entier}), on accède à l'élément de rang {entier} dans une liste (au lieu d'une Map)
        def matchIntegerCurrentKey = currentKey =~ this.regexpIntegerKey
        currentKey = matchIntegerCurrentKey ? matchIntegerCurrentKey[0][1] as int : currentKey
        def nextKey = i == keysCount - 1 ? null : keys[i + 1]
                
        // Si on est arrivé au bout du chemin, on quitte la boucle
        if (nextKey == null) {
          break
        }
    
        // Construit au préalable le chemin s'il n'existe pas dans le référentiel
        def currentKeyExists = matchIntegerCurrentKey ? currentKey < currentHolder.size()
            : currentKey in currentHolder          
        def nextHolder = nextKey =~ regexpIntegerKey ? [] : [ : ]        
        def areHoldersCompatible = (currentHolder[currentKey] instanceof Map && nextHolder instanceof Map)
            || (currentHolder[currentKey] instanceof List && nextHolder instanceof List)
          
        if (! (currentKeyExists && areHoldersCompatible)) {
          currentHolder[currentKey] = nextHolder
        }
        
        if (WIP) {
          // ...
          subPath = matchIntegerCurrentKey ? "$subPath/($currentKey)" : "$subPath/$currentKey"
          def elem = currentHolder[currentKey]
          
          def type = elem.getClass().superclass.interfaces[0].getSimpleName()
          index[subPath] = "${type} *=${elem.size()}"
    
          println "$subPath -> ${this.index[subPath]}"
          // ...
        }
        
        currentHolder = currentHolder[currentKey]
      }
      
      // Définit un élément dans le référentiel      
      // L'élément est positionné au niveau du chemin indiqué en paramètre
      // S'il existe déjà un élément défini au niveau du chemin, il est remplacé 
      currentHolder[currentKey] = element
    
      if (WIP) {
        // ...
        subPath = "$subPath/$currentKey"
        def elem = currentHolder[currentKey]
        
        def type = element == null ? "(${element})" : "${element.getClass().getSimpleName()}"    
        index[subPath] = type
    
        println "$subPath -> ${this.index[subPath]}"
        // ...
      }
    
      if (! WIP) {
        // L'index des éléments du référentiel est mis à jour
        this.index = [ : ]
        this.updateIndex(this.index, ".", this.entries)
      }
    
      // Recherche des chemins des objets du métamodèle
      def metaPaths = this.findPaths("${path}(/.*)*/@meta")
    
      // Pour chaque chemin trouvé
      for(metaPath in metaPaths) {
        // On retrouve l'objet du métamodèle
        def objPath = this.getParentPath(metaPath)
        def obj = this.getElement(objPath)
        def module = obj.'@meta'
        def moduleInstance = null
    
        if (module in this.modules) {
          // Si le résultat du chargement du module a déjà été mis en cache, on le réutilise
          moduleInstance = this.modules[module]
        }
        else {
          // Sinon on recherche le module
          def moduleFile = new File(this.modulesPath + module.replaceAll("\\.", "/") + ".groovy")
    
          if (moduleFile.exists()) {
            // Si le module existe on le charge & on l'initialise
            moduleInstance = this.class.classLoader.parseClass(moduleFile).newInstance()
            moduleInstance.initModule(this)
          }
          // On met en cache le résultat du chargement du module (null si le module n'a pas été trouvé)
          this.modules[module] = moduleInstance
        }
    
        // Si le module a été trouvé
        if (moduleInstance != null) {
          // On gère la validation des objets du métamodèle
          moduleInstance.raiseErrorIfElementIsInvalid(objPath, obj)
    
          // Sur ajout d'objets valides du métamodèle
          moduleInstance.onValidElement(objPath, obj)
        }
      }
    
      this.currentPath = path
    
      if (isDefaultPath) {
        this.currentDefaultPath = this.currentPath
      }
    
      return this
    }

    def setYamlElement(path, yamlElement)
    {
      // L'élément YAML est converti en chaine de caractères
      def yamlSrc = yamlElement.toString()
      
      // Les tabulations sont acceptés comme indentation (puis sont convertis en espaces)
      yamlSrc = yamlSrc.replace('-\t', '- ').replace('\t', '  ')
    
      // L'élément est obtenu depuis le source YAML
      def element = this.yamlParser.parseText(yamlSrc)
      
      // Définit l'élément dans le référentiel
      return this.setElement(path, element)      
    }

    def setProjectElementFromFile(projectFilename)
    {
      def projectFile  = new File(projectFilename)
    
      if (! projectFile.exists()) {
        throw new RuntimeException("### FICHIER PROJET INEXISTANT: projectFilename=$projectFilename")
      }
    
      def projectDirFile    = projectFile.getParentFile()
      def repositoryFilename  = "${projectDirFile.absolutePath}/work/${projectDirFile.name}-repository.yaml"
    
      this.setElement("@projectDirectory", projectDirFile.absolutePath)    
        .setYamlElement(null, projectFile.text)
        .save(repositoryFilename)
        
      return this
    }

    static def main(args)
    {
      def projectFilename = args[0]
      
      Repository.create()
        .setProjectElementFromFile(projectFilename)
    }

    def save(filename=null)
    {
      // Le chemin du fichier est stocké si renseigné
      if (filename != null) {
        if (filename.trim() != "") {          
          this.saveToFilename = filename
        }
      }
    
      // Le chemin du fichier est obligatoire la première fois
      if (this.saveToFilename == null) {
        throw new RuntimeException("### [save] LE FICHIER EST OBLIGATOIRE")
      }
      
      def file = new File(this.saveToFilename)
      
      // Le chemin du fichier doit exister
      if (! file.getParentFile().exists()) {
        throw new RuntimeException("### [save] LE CHEMIN DU FICHIER N'EXISTE PAS: ${file}")
      }
    
      file.write(this.toString())
      
      return this
    }

    def load(filename)
    {
      // Le chemin du fichier est obligatoire
      if (filename == null || filename.trim() == "") {
        throw new RuntimeException("### [load] LE FICHIER EST OBLIGATOIRE")
      }
      
      // Le chemin du fichier doit exister
      if (! new File(filename).exists()) {
        throw new RuntimeException("### [load] LE FICHIER N'EXISTE PAS: ${filename}")
      }
    
      this.saveToFilename = filename
      def map = yamlParser.parseText(new File(this.saveToFilename).text)
      
      this.toMap().keySet().each { k ->
        if (! k.startsWith('@')) {
          this.(k) = map.(k)
        }
      }
    
      return this
    }

    def getElement(path, required=true)
    {
      // Le chemin est obligatoire
      if (path == null || path.toString().trim() == "") {
        throw new RuntimeException("### LE CHEMIN EST OBLIGATOIRE: path=${path}")
      }
      
      // Si le chemin n'est pas une chaine de caractères, un chemin est créé de la forme: ./{path.toString()}
      if (! (path instanceof String)) {
        path = "./${path.toString()}".toString()
      }
    
      def element = this.entries
      def keys = path.split("/")
      def keysCount = keys.size()
      def keyAsString = null
      def currentKey = null
      def currentKeyExists = false
    
      for (def i = 0; i < keysCount; i++) {
        keyAsString = currentKey = keys[i]
    
        // On ignore la pseudo-clé: courante
        if (currentKey == ".") {
          continue
        }
    
        // Si la clé à la forme ({entier}), on accède à l'élément de rang {entier} dans une liste (au lieu d'une Map)
        def matchIntegerCurrentKey = currentKey =~ this.regexpIntegerKey
        currentKey = matchIntegerCurrentKey ? matchIntegerCurrentKey[0][1] as int : currentKey
        currentKeyExists = matchIntegerCurrentKey ? currentKey < element.size() : currentKey in element          
        
        // Récupère l'élément positionné au niveau du chemin indiqué en paramètre
        element = element[currentKey]
    
        if (element == null) {
          break
        }
      }
    
      // Si required vaut true et l'élément n'est pas trouvé: lance une RuntimeException, sinon retourne null
      if (element == null && required) {
        throw new RuntimeException(currentKeyExists
          ? "### ELEMENT OBLIGATOIRE A L'EMPLACEMENT: path=${path}"
          : "### ELEMENT [${keyAsString}] NON TROUVE DANS L'EMPLACEMENT: path=${path}")
      }
          
      // Lit un élément dans le référentiel
      return element
    }

    def findPaths(pathSelector, valueSelector="[SKIP_VALUE_SELECTION]")
    {
      if (pathSelector == null || pathSelector.toString().trim() == "" || pathSelector == "*") {
        // Si le sélecteur de chemin est non renseigné, tous les chemins sont retournés
        // Si le sélecteur de chemin est "*", tous les chemins sont retournés
        pathSelector = ".*"
      }
      else {
        // Si le sélecteur de chemin contient "((@ESC:chaine de caractères))",
        // cette partie doit être transformée en expression régulière
        pathSelector = escapeStringPartsAsRegexps(pathSelector)
      }
    
      // Recherche de chemins dans l'index du référentiel
      // Les chemins retournés correspondent en totalité à l'expression régulière de sélection des chemins
      def paths = this.index.findAll{ k, v -> k.matches(pathSelector) }
                  .collect{ k, v -> k }
                  
      // Si le sélecteur de valeur vaut "[SKIP_VALUE_SELECTION]",
      // on NE SELECTIONNE PAS par rapport à la valeur des chemins
      if (valueSelector == "[SKIP_VALUE_SELECTION]") {
        return paths
      }
      
      // Si le sélecteur de valeur contient "((@ESC:chaine de caractères))",
      // cette partie doit être transformée en expression régulière
      valueSelector = escapeStringPartsAsRegexps(valueSelector)
    
      // La sélection de valeur se fait par rapport aux valeurs (au format chaine caractères) associées aux chemins
      paths = paths.findAll{ path ->
        def valueAsString = getElement(path) as String
        valueAsString.matches(valueSelector)
      }
    
      return paths
    }

    private def escapeStringPartsAsRegexps(str, reDelimiter=/\(\(@ESC:(.+?)\)\)/)
    {
      // Si la chaine est nulle, on la retourne telle quelle
      if (str == null) {
        return str
      }
      
      def matches = str =~ reDelimiter
      def reElements = [ '\\', '/', '^', '.', '*', '+', '?', '|', '{', ',', '}', '[', '-', ']', '$', '(', ')' ]
    
      for (match in matches) {
        def from  = match[0]
        def to    = match[1]
                
        // Convertir les parties d'une chaine de caractères en expressions régulières
        for (reElement in reElements) {
          to = to.replace(reElement, "\\$reElement")
        }
        str = str.replace(from, to)
      }
      
      return str
    }

    def getParentPath(path, count=1)
    {
      // Le chemin est obligatoire
      if (path == null || path.toString().trim() == "") {
        throw new RuntimeException("### LE CHEMIN EST OBLIGATOIRE: path=${path}")
      }
      
      // On remonte dans les parents dans la limite du compteur
      for (def i = 0; i < count; i++) {
        def p = path.lastIndexOf("/")
        
        // On remonte dans les parents dans la limite de la racine du chemin
        if (p == -1) {
          break
        }
        
        // Obtenir le chemin parent d'un chemin
        path = path.substring(0, p)
      }
      
      return path
    }

    def toMap()
    {
      return [
        '@meta':        'meta.design.Object',
        '@class':        'model.Repository',
        defaultPathsSequence:  this.defaultPathsSequence,  
        modules:        this.modules.keySet(),
        entries:        this.entries,
        index:          this.index,
      ]
    }

    String toString()
    {
      this.yamlBuilder(this.toMap())
    
      return this.yamlBuilder.toString()
    }

    def displayIndex(path=".", showValues=true)
    {
      def lg    = 160
      def lines  = []
    
      this.index.each { currentPath, type ->
        if (currentPath.startsWith(path)) {
          def value = ''
    
          if (! type.contains(' *=')) {  // Collection
            if (showValues) {
              value = "=${this.getElement(currentPath, false)}"
            }
          }
    
          def displayedPath = currentPath.replace(path, '.')
          lines.add("${displayedPath} -> ${type}${value}")
        }
      }
      println('=' * lg)
      println("  INDEX CONTEXT: ${path}")
      println('=' * lg)
      println(lines.join("\n"))
      println('=' * lg)
      println()
    
      return this
    }

    private def updateIndex(index, path, element)
    {      
      if (element instanceof List) {
        // Pour les éléments de type collection, on indique en plus le nombre d'occurrence
        def type = element.getClass().superclass.interfaces[0].getSimpleName()
        index[path] = "${type} *=${element.size()}"
    
        element.eachWithIndex { e, i ->
          // Construit tous les chemins d'accès aux éléments (et sous-éléments) du référentiel
          updateIndex(index, "${path}/(${i})", e)
        }
      }
      else if (element instanceof Map) {
        // Pour les éléments de type collection, on indique en plus le nombre d'occurrence
        def type = element.getClass().superclass.interfaces[0].getSimpleName()
        index[path] = "${type} *=${element.size()}"
    
        element.each { k, v ->
          // Construit tous les chemins d'accès aux éléments (et sous-éléments) du référentiel
          updateIndex(index, "${path}/${k}", v)
        }
      }
      else {
        // Mise à jour de l'index des éléments du référentiel
        // A chaque chemin d'accès est associé le type d'élément auquel le chemin donne accès
        def type = element == null ? "(${element})" : "${element.getClass().getSimpleName()}"    
        index[path] = "${type}"
      }
    
      return this
    }

    def manageModuleDelegatedOperation(element, operation, path)
    {  
      // Gère la délégation d'une opération à l'éventuel module d'un élément
      
      // Si l'objet n'a pas d'attribut @meta, la délégation a échoué
      if (element instanceof Map && '@meta' in element) {
        def meta = element.'@meta'
        
        // Si la valeur de @meta n'existe pas dans le cache des modules, la délégation a échoué
        if (meta in modules) {
          def instance = modules[meta]
          
          // Si le module trouvé ne possède pas de méthode correspondant à l'opération à déléguer, la délégation a échoué
          if (instance.metaClass.respondsTo(instance, operation)) {
            // L'opération est appelée en fournissant en paramètres: path & element
            // Retourne [ isSuccess: true, result: valeur ] si la délégation réussit
            return [ 'isSuccess' : true, 'result' : instance."${operation}"(path, element) ]
          }
        }
      }
    
      // Retourne [ isSuccess: false ] si la délégation échoue
      return [ 'isSuccess' : false ]
    }
}
