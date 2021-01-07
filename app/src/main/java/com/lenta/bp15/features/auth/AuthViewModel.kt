package com.lenta.bp15.features.auth

import androidx.lifecycle.MutableLiveData
import com.lenta.bp15.platform.navigation.IScreenNavigator
import com.lenta.bp15.repository.IRepoInMemoryHolder
import com.lenta.bp15.repository.requests.INetRequestsRepository
import com.lenta.bp15.repository.requests.pojo.UserPermissionsParams
import com.lenta.shared.account.ISessionInfo
import com.lenta.shared.exception.Failure
import com.lenta.shared.features.login.CoreAuthViewModel
import com.lenta.shared.features.login.isEnterEnabled
import com.lenta.shared.features.login.isValidLoginFields
import com.lenta.shared.requests.network.Auth
import com.lenta.shared.requests.network.AuthParams
import com.lenta.shared.settings.IAppSettings
import com.lenta.shared.utilities.Logg
import com.lenta.shared.utilities.extentions.combineLatest
import com.lenta.shared.utilities.extentions.launchUITryCatch
import com.lenta.shared.utilities.extentions.map
import com.lenta.shared.utilities.getBaseAuth
import com.lenta.shared.utilities.runIfDebug
import javax.inject.Inject

class AuthViewModel : CoreAuthViewModel() {

    @Inject
    lateinit var auth: Auth

    @Inject
    lateinit var navigator: IScreenNavigator

    @Inject
    lateinit var sessionInfo: ISessionInfo

    @Inject
    lateinit var appSettings: IAppSettings

    @Inject
    lateinit var netRequests: INetRequestsRepository

    @Inject
    lateinit var repoInMemoryHolder: IRepoInMemoryHolder


    val packageName = MutableLiveData<String>()

    override val enterEnabled: MutableLiveData<Boolean> by lazy {
        login.combineLatest(password).map { isValidLoginFields(login = it?.first, password = it?.second) }
                .combineLatest(progress).map { isEnterEnabled(isFieldsValid = it?.first, inProgress = it?.second) }
    }

    init {
        launchUITryCatch {
            sessionInfo.isAuthSkipped.value = false
            sessionInfo.packageName = packageName.value
        }
    }

    override fun onClickEnter() {
        launchUITryCatch {
            progress.value = true
            auth(AuthParams(getLogin(), getPassword())).either(::handleFailure, ::loadPermissions)
        }
    }

    private fun loadPermissions(@Suppress("UNUSED_PARAMETER") b: Boolean) {
        launchUITryCatch {
            getLogin().let { login ->
                sessionInfo.userName = login
                sessionInfo.basicAuth = getBaseAuth(login, getPassword())
                appSettings.lastLogin = login
                netRequests.getUserPermissions(UserPermissionsParams(
                        userName = login
                )).either(::handleFailure) {
                    repoInMemoryHolder.storesRequestResult = it
                    onAuthSuccess(login)
                }
            }
        }
    }

    private fun onAuthSuccess(login: String) {
        progress.value = false
        navigator.openFastDataLoadingScreen()
    }

    override fun handleFailure(failure: Failure) {
        sessionInfo.isAuthSkipped.value = false
        progress.value = false
        navigator.openAlertScreen(failure, pageNumber = "97")
        Logg.e { "handleFailure: $failure" }
    }

    override fun onClickAuxiliaryMenu() {
        navigator.openAuxiliaryMenuScreen()
    }

    private fun getLogin(): String {
        return login.value?.trim().orEmpty()
    }

    private fun getPassword(): String {
        return password.value?.trim().orEmpty()
    }

    override fun onResume() {
        launchUITryCatch {
            if (!appSettings.lastLogin.isNullOrEmpty()) {
                login.value = appSettings.lastLogin
            }
            runIfDebug {
                Logg.d { "login.value ${login.value}" }
                if (login.value.isNullOrEmpty()) {
                    login.value = "QWERTY"
                }
                if (login.value == "QWERTY" && getPassword().isEmpty()) {
                    password.value = "12345"
                }
            }

        }
    }

}