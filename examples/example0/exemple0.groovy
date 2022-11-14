import java.time.*

import org.openqa.selenium.*
import org.openqa.selenium.firefox.*
import org.openqa.selenium.support.ui.*
//______________________________________________________________________
//
println("")
username	= new String(System.console().readLine(">>> GitHub Username: "))
password	= new String(System.console().readPassword(	">>> GitHub Password: "))
println("")
//______________________________________________________________________
//
System.setProperty("webdriver.gecko.driver", "../../firefox-driver/geckodriver")

driver		= new FirefoxDriver()
driverWait	= new WebDriverWait(driver, Duration.ofSeconds(30))

try {
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

	driver.quit()
}
//______________________________________________________________________
//
def scenario() {

	driver.get("https://github.com/")
	
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

	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.xpath("//summary[ ./@aria-label = 'View profile and more' ]")))
			.click()

	driverWait.until(ExpectedConditions.visibilityOfElementLocated(
		By.xpath("//button[ contains(./text(), 'Sign out') ]")))
			.click()

	println("-" * 160)
	print(content)
	println("-" * 160)
}
//______________________________________________________________________
//
