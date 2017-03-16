package app.controllers

import app.handlers.UserHandler
import database.daos.DAOManager
import database.daos.ResetPasswordDAO
import database.daos.UserDAO
import extensions.managedRedirect
import j2html.TagCreator.h1
import mu.KLogging
import spark.ModelAndView
import spark.Request
import spark.Response
import spark.Session
import utils.Utils
import utils.Validation
import utils.j2htmlPartials
import java.util.*

/**
 * Created by alewis on 12/03/2017.
 */
class ResetPasswordController : Controller {

    companion object : KLogging()

    override fun initSessionAttributes(session: Session) {
        hashMapOf(Pair("new_password_field_error", false), Pair("new_password_repeated_field_error", false)).forEach { key, value -> if (!session.attributes().contains(key)) session.attribute(key, value)}
    }

    fun generateResetPasswordPageContent(request: Request, username: String, model: HashMap<String, Any>, authHash: String) {
        if (UserHandler.isLoggedIn(request) && UserHandler.loggedInUsername(request) == username) {
            Web.loadNavBar(request, model)
        }
        val resetPasswordForm = j2htmlPartials.pureFormAligned_ResetPassword(request.session(), "reset_password_form", "/reset_password/$username/$authHash", "post")
        model.put("reset_password_form", h1("Reset Password").render() + resetPasswordForm.render())
    }

    fun generateAccessDeniedContent(request: Request, model: HashMap<String, Any>) {
        model.put("unauthorised_reset_request_message", h1("Access Denied"))
    }

    override fun get(request: Request, response: Response, layoutTemplate: String): ModelAndView {
        val model = HashMap<String, Any>()
        model.put("template", "/templates/reset_password.vtl")
        model.put("title", "Thames Valley Furs - Reset password")
        logger.info("${UserHandler.getSessionIdentifier(request)} -> Received GET request for RESET_PASSWORD/${request.params(":username")} page")

        val username = request.params(":username")
        val authHash = request.params(":authhash")

        val resetPasswordDAO = DAOManager.getDAO(DAOManager.TABLE.RESET_PASSWORD) as ResetPasswordDAO
        val userDAO = DAOManager.getDAO(DAOManager.TABLE.USERS) as UserDAO

        if (username != null) {
            if (authHash == null) {
                if (UserHandler.isLoggedIn(request) && UserHandler.loggedInUsername(request) == username) {
                    val newAuthHash = Utils.randomHash()
                    val userId = userDAO.getUserID(username)
                    if (resetPasswordDAO.authHashExists(userId)) {
                        resetPasswordDAO.updateAuthHash(userId, newAuthHash)
                    } else {
                        resetPasswordDAO.insertAuthHash(userId, newAuthHash)
                    }
                    response.managedRedirect(request, "/reset_password/$username/$newAuthHash")
                } else {
                    logger.info("${UserHandler.getSessionIdentifier(request)} -> Received unauthorised reset password request for user $username")
                    generateAccessDeniedContent(request, model)
                }
            } else {
                if (resetPasswordDAO.authHashExists(userDAO.getUserID(username))) {
                    if (resetPasswordDAO.getAuthHash(userDAO.getUserID(username)) == authHash) {
                        logger.info("${UserHandler.getSessionIdentifier(request)} -> Received authorised reset password request for user $username")
                        generateResetPasswordPageContent(request, username, model, authHash)
                    } else {
                        response.managedRedirect(request, "/reset_password/$username")
                    }
                } else {
                    response.managedRedirect(request, "/reset_password/$username")
                }
            }
        }
        return ModelAndView(model, layoutTemplate)
    }

    private fun post_resetPassword(request: Request, response: Response): Response {
        if (Web.getFormHash(request.session(), "reset_password_form") == request.queryParams("hashid")) {
            val newPassword = request.queryParams("new_password")
            val newPasswordRepeated = request.queryParams("new_password_repeated")

            val newPasswordInputIsValid = Validation.matchPasswordPattern(newPassword)
            val newRepeatedPasswordIsValid = Validation.matchPasswordPattern(newPasswordRepeated)

            if (!newPasswordInputIsValid) request.session().attribute("new_password_field_error", true) else request.session().attribute("new_password_field_error", false)
            if (!newRepeatedPasswordIsValid) request.session().attribute("new_password_repeated_field_error", true) else request.session().attribute("new_password_repeated_field_error", false)

            if (newPasswordInputIsValid && newRepeatedPasswordIsValid) {
                if (newPassword == newPasswordRepeated) { println("Passwords match") }
            }
        }
        return response
    }

    override fun post(request: Request, response: Response): Response {
        when (request.queryParams("formName")) {
            "reset_password_form" -> return post_resetPassword(request, response)
        }
        return response
    }
}