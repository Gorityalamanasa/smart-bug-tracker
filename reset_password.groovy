import jenkins.model.Jenkins
import hudson.security.HudsonPrivateSecurityRealm

def instance = Jenkins.getInstance()
def realm = instance.getSecurityRealm()
def account = realm.createAccount("Manasa", "Manasa@3307")
account.save()
println("Password reset successfully for user: Manasa")
