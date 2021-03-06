/*
 * # DON'T BE A DICK PUBLIC LICENSE
 *
 * > Version 1.1, December 2016
 *
 * > Copyright (C) 2016-2017 Adam Prakash Lewis
 *
 *  Everyone is permitted to copy and distribute verbatim or modified
 *  copies of this license document.
 *
 * > DON'T BE A DICK PUBLIC LICENSE
 * > TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *  1. Do whatever you like with the original work, just don't be a dick.
 *
 *      Being a dick includes - but is not limited to - the following instances:
 *
 * 	 1a. Outright copyright infringement - Don't just copy this and change the name.
 * 	 1b. Selling the unmodified original with no work done what-so-ever, that's REALLY being a dick.
 * 	 1c. Modifying the original work to contain hidden harmful content. That would make you a PROPER dick.
 *
 *  2. If you become rich through modifications, related works/services, or supporting the original work,
 *  share the love. Only a dick would make loads off this work and not buy the original work's
 *  creator(s) a pint.
 *
 *  3. Code is provided with no warranty. Using somebody else's code and bitching when it goes wrong makes
 *  you a DONKEY dick. Fix the problem yourself. A non-dick would submit the fix back.
 */



package app.core.controllers

import api.core.TacusciAPI
import app.core.Web
import app.core.handlers.GroupHandler
import app.core.handlers.UserHandler
import extensions.isNullOrBlankOrEmpty
import extensions.managedRedirect
import mu.KLogging
import spark.ModelAndView
import spark.Request
import spark.Response
import spark.Session
import utils.Validation
import utils.j2htmlPartials

/**
 * Created by alewis on 27/10/2016.
 */

class LoginController : Controller {

    companion object : KLogging()

    override var rootUri: String = "/login"
    override val childGetUris: MutableList<String> = mutableListOf()
    override val childPostUris: MutableList<String> = mutableListOf()
    override val templatePath: String = "/templates/login.vtl"
    override val pageTitleSubstring: String = "Login"
    override val handlesGets: Boolean = true
    override val handlesPosts: Boolean = true

    override fun initSessionBoolAttributes(session: Session) {
        hashMapOf(Pair("login_incorrect_creds", false), Pair("is_banned", false), Pair("username", ""), Pair("password", ""),
                Pair("banned_username", ""), Pair("login_error", false)).forEach { key, value -> if (!session.attributes().contains(key)) session.attribute(key, value) }
    }

    override fun get(request: Request, response: Response, layoutTemplate: String): ModelAndView {
        logger.info("${UserHandler.getSessionIdentifier(request)} -> Received GET request for LOGIN page")
        val model = hashMapOf<String, Any>()

        TacusciAPI.injectAPIInstances(request, response, model)

        Web.insertPageTitle(request, model, pageTitleSubstring)
        Web.loadNavigationElements(request, model)

        if (UserHandler.isLoggedIn(request)) {
            logger.info("${UserHandler.getSessionIdentifier(request)} -> User already logged in, redirecting to landing page")
            val loggedInUsername = UserHandler.loggedInUsername(request)
            if (GroupHandler.userInGroup(loggedInUsername, "dashboard_access"))
                response.managedRedirect(request, "/dashboard")
            else
                response.managedRedirect(request, "/")
        }

        model.put("template", templatePath)

        val loginForm = j2htmlPartials.pureFormAligned_Login(request.session(), "login_form", rootUri, "post")

        if (request.session().attribute("login_incorrect_creds")) {
            request.session().attribute("login_incorrect_creds", false)
            model.put("usernameOrPasswordIncorrect", true)
        }

        model.put("login_form", loginForm.render())
        //model.put("login_form", h1("Login").render() + loginForm.render())

        if (request.session().attribute("is_banned")) {
            logger.info("${UserHandler.getSessionIdentifier(request)} -> User ${request.session().attribute<String>("banned_username")} is banned")
            model.put("login_form", "")
            model.put("signup_link", "")
            request.session().attribute("is_banned", false)
            request.session().attribute("banned_username", "")
        } else {
            model.put("userIsNotBanned", "true")
        }
        return ModelAndView(model, layoutTemplate)
    }

    override fun post(request: Request, response: Response): Response {
        when (request.queryParams("formName")) {
            "login_form" -> return post_postLogin(request, response)
            "sign_out_form" -> return post_logout(request, response)
        }
        //if none of the form names match go back to this page...
        response.managedRedirect(request, rootUri)
        return response
    }

    private fun post_postLogin(request: Request, response: Response): Response {
        logger.info("${UserHandler.getSessionIdentifier(request)} -> Received POST submission for LOGIN page")
        if (Web.getFormHash(request, "login_form") == request.queryParams("hashid")) {
            var username = request.queryParams("username")
            var email = ""
            val password = request.queryParams("password")

            if (username.isNullOrBlankOrEmpty() || password.isNullOrBlankOrEmpty()) {
                request.session().attribute("login_error", true)
                logger.info("Unrecognised username/password provided in form")
                response.managedRedirect(request, rootUri)
                return response
            } else {
                //if the format for username is incorrect
                if (!Validation.matchUsernamePattern(username)) {
                    //check to see if it's an email address
                    if (Validation.matchEmailPattern(username)) {
                        //get username that corresponds to email
                        email = username
                        username = UserHandler.userDAO.getUsernameFromEmail(email)
                    }
                }
                UserHandler.login(request, username, password)
            }
        } else {
            logger.warn("${UserHandler.getSessionIdentifier(request)} -> has submitted an invalid login form...")
        }

        logger.info("${UserHandler.getSessionIdentifier(request)} -> Redirecting to login page")
        response.managedRedirect(request, rootUri)
        return response
    }

    private fun post_logout(request: Request, response: Response): Response {
        logger.info("${UserHandler.getSessionIdentifier(request)} -> Received POST submission for LOGOUT form")
        if (Web.getFormHash(request, "sign_out_form") == request.queryParams("hashid")) {
            if (UserHandler.isLoggedIn(request)) {
                UserHandler.logout(request)
            }
            response.managedRedirect(request, "/")
            return response
        }
        response.managedRedirect(request, rootUri)
        return response
    }
}