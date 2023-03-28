def a = request.getParameter("a");
def b = request.getParameter("b");

def r = null //	(a as int) + (b as int)

def user = "user"
def password = "pass"
def soapUrl = "http://www.dneonline.com/calculator.asmx"
def soapRequestBody = """
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:tem="http://tempuri.org/">
   <soapenv:Header/>
   <soapenv:Body>
      <tem:Add>
         <tem:intA>${a}</tem:intA>
         <tem:intB>${b}</tem:intB>
      </tem:Add>
   </soapenv:Body>
</soapenv:Envelope>
"""

def post = new URL(soapUrl).openConnection()
def postRC = null
def postResponse = null

try {
	def auth = user + ":" + password
	def encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes("UTF-8"))
	def authHeaderValue = "Basic " + new String(encodedAuth)
	
	post.setRequestMethod("POST")
	post.setRequestProperty("Authorization", authHeaderValue)
	post.setRequestProperty("Content-Type", "text/xml")
	post.setDoOutput(true)
	post.getOutputStream().write(soapRequestBody.getBytes("UTF-8"))

	postRC = post.getResponseCode()

	postResponse = post.getInputStream().getText()
	r = postResponse
}
catch (IOException e) {
	e.printStackTrace()
}
finally {
}
   
println """
<html>
    <head>
        <title>Groovy Servlet Example</title>
    </head>
    <body>
        <h1>Calculer - ${new Date()}</h1>
		<form method="POST" action="a.groovy">
			<p>a: <input name="a" value="${a}"/></p>
			<p>b: <input name="b" value="${b}"/></p>
			<p><input type="submit" value="Calculer"></p>
			<p>r=${r}</p>
			<p>RC=${postRC}</p>
		</form>
    </body>
</html>
"""
