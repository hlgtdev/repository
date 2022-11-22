import java.time.*

import org.openqa.selenium.*
import org.openqa.selenium.remote.*
import org.openqa.selenium.remote.http.*
import org.openqa.selenium.firefox.*
import org.openqa.selenium.support.ui.*
//______________________________________________________________________
//
public static RemoteWebDriver createDriverFromSession(final String sessionId, URL command_executor){

	CommandExecutor executor = new HttpCommandExecutor(command_executor) {

		@Override
		public Response execute(Command command) throws IOException {
			
			Response response = null;

			if (command.getName() == "newSession") {
				response = new Response();
				response.setSessionId(sessionId.toString());
				response.setStatus(0);
				response.setValue(Collections.<String, String>emptyMap());

				try {
					def commandCodec = null;
					commandCodec = this.getClass().getSuperclass().getDeclaredField("commandCodec");
					commandCodec.setAccessible(true);
					commandCodec.set(this, new W3CHttpCommandCodec());

					def responseCodec = null;
					responseCodec = this.getClass().getSuperclass().getDeclaredField("responseCodec");
					responseCodec.setAccessible(true);
					responseCodec.set(this, new W3CHttpResponseCodec());
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			} else {
				response = super.execute(command);
			}
			return response;
		}
	};

	return new RemoteWebDriver(executor, new DesiredCapabilities());
}
//______________________________________________________________________
//
println("")
username	= new String(System.console().readLine(">>> GitHub Username: "))
password	= new String(System.console().readPassword(	">>> GitHub Password: "))
println("")
//______________________________________________________________________
//
System.setProperty("webdriver.gecko.driver", "../../firefox-driver/geckodriver")

f = new File("./selenium.session")

svr = null
sid = null
driver = null

try {
	if (f.exists()) {
		lines = f.readLines()		
		svr = new URL(lines[0])
		sid = lines[1]
		
		println ">>> $svr\n>>> $sid"
		
		driver = createDriverFromSession(sid, svr);
	}
	else {
		driver	= new FirefoxDriver()
		svr	= driver.getCommandExecutor().getAddressOfRemoteServer().toString();
		sid	= driver.getSessionId().toString();

		println ">>> $svr\n>>> $sid"

		f.write("""$svr
$sid
""")
	}
	
	driverWait	= new WebDriverWait(driver, 5)

	scenario()
}
catch(Exception e) {
	println("")
	println("#" * 160)
	e.printStackTrace()
	println("#" * 160)
}
finally {
	println("")
	System.console().readLine '>>> Press [Enter]...'
	println("")

//	driver.quit()
}
//______________________________________________________________________
//
def scenario() {

	driver.get("https://github.com/")
/*	
	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.linkText("Sign in")))
			.click()

	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.id("login_field")))
			.sendKeys(username)

	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.id("password")))
			.sendKeys(password)

	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.xpath("//input[ ./@name = 'commit' ]")))
			.click()
*/
	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.linkText("${username}/first-repo")))
			.click()

	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.linkText("xmprog")))
			.click()

	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.linkText("xmprog-runner.py")))
			.click()

	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.linkText("Raw")))
			.click()

	content = driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.xpath("//pre")))
			.getAttribute("innerText")

	driver.navigate().back();
/*
	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.xpath("//summary[ ./@aria-label = 'View profile and more' ]")))
			.click()

	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.xpath("//button[ contains(./text(), 'Sign out') ]")))
			.click()
*/
	println("-" * 160)
	print(content)
	println("-" * 160)
}
//______________________________________________________________________
//
