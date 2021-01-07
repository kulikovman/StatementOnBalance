package com.lenta.bp15.features.task_list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.lenta.bp15.model.ITaskManager
import com.lenta.bp15.model.pojo.Task
import com.lenta.bp15.platform.navigation.IScreenNavigator
import com.lenta.bp15.platform.resource.IResourceManager
import com.lenta.shared.account.ISessionInfo
import com.lenta.shared.platform.viewmodel.CoreViewModel
import com.lenta.shared.utilities.BlockType
import com.lenta.shared.utilities.databinding.OnOkInSoftKeyboardListener
import com.lenta.shared.utilities.databinding.PageSelectionListener
import com.lenta.shared.utilities.extentions.combineLatest
import com.lenta.shared.utilities.extentions.launchUITryCatch
import com.lenta.shared.utilities.extentions.mapSkipNulls
import javax.inject.Inject

class TaskListViewModel : CoreViewModel(), PageSelectionListener, OnOkInSoftKeyboardListener {

    @Inject
    lateinit var navigator: IScreenNavigator

    @Inject
    lateinit var sessionInfo: ISessionInfo

    @Inject
    lateinit var resource: IResourceManager

    @Inject
    lateinit var manager: ITaskManager


    /**
    Переменные
     */

    val title by lazy {
        resource.tk(sessionInfo.market.orEmpty())
    }

    val processingField by lazy {
        MutableLiveData(sessionInfo.userName)
    }

    val searchField = MutableLiveData("")

    val requestFocusToProcessingField = MutableLiveData(false)

    val requestFocusToSearchField = MutableLiveData(false)

    val processingList by lazy {
        manager.processingTasks.combineLatest(processingField).mapSkipNulls { it.first }.map(listFilterFunc)
    }

    val searchList by lazy {
        manager.searchTasks.combineLatest(searchField).mapSkipNulls { it.first }.map(listFilterFunc)
    }

    private val listFilterFunc = { tasks: List<Task> ->
        when {
            isEnteredLogin() -> tasks
            else -> tasks.filter { task -> task.number.contains(getEnteredValue().orEmpty()) }
        }.let { list ->
            list.mapIndexed { index, task ->
                task.convertToItemTaskUi(index)
            }
        }
    }

    /**
    Блок инициализации
     */

    init {
        launchUITryCatch {
            updateTaskList()
        }
    }

    /**
    Методы
     */

    override fun onPageSelected(position: Int) {
        selectedPage.value = position
    }

    fun isNeedUpdateTaskList(): Boolean {
        return manager.isNeedUpdateTaskList
    }

    override fun onOkInSoftKeyboard(): Boolean {
        selectedPage.value?.let { page ->
            when (page) {
                0 -> updateProcessingList()
                1 -> updateSearchList()
                else -> throw IllegalArgumentException("Wrong pager position!")
            }
        }
        return true
    }

    private fun updateProcessingList() {
        when{
            isEnteredLogin() -> manager.loadProcessingTaskList(getEnteredValue())
            getEnteredValue().isNullOrEmpty() -> manager.loadProcessingTaskList()
        }
    }

    private fun updateSearchList() {
        when{
            isEnteredLogin() -> manager.loadSearchTaskList(value = getEnteredValue())
            getEnteredValue().isNullOrEmpty() -> manager.loadSearchTaskList()
        }
    }

    private fun isEnteredLogin(): Boolean {
        return getEnteredValue()?.let { numberOrLogin ->
            numberOrLogin.isNotEmpty() && !numberOrLogin.all { it.isDigit() }
        } ?: false
    }

    private fun getEnteredValue(): String? {
        return when (selectedPage.value) {
            0 -> processingField.value
            1 -> searchField.value
            else -> throw IllegalArgumentException("Wrong pager position!")
        }
    }

    fun onClickItemProcessingPosition(position: Int) {
        processingList.value?.getOrNull(position)?.number?.let { taskNumber ->
            manager.processingTasks.value?.find { it.number == taskNumber }?.let { task ->
                prepareToOpenTask(task)
            }
        }
    }

    fun onClickItemSearchPosition(position: Int) {
        searchList.value?.getOrNull(position)?.number?.let { taskNumber ->
            manager.searchTasks.value?.find { it.number == taskNumber }?.let { task ->
                prepareToOpenTask(task)
            }
        }
    }

    private fun prepareToOpenTask(task: Task) {
        with(task) {
            when (block.type) {
                BlockType.LOCK -> navigator.showAlertBlockedTaskAnotherUser(block.user, block.ip)
                BlockType.SELF_LOCK -> navigator.showAlertBlockedTaskByMe { openTaskCard(task) }
                else -> openTaskCard(task)
            }
        }
    }

    private fun openTaskCard(task: Task) {
        manager.updateCurrentTask(task)
        navigator.openTaskCardScreen()
    }

    fun onClickUpdate() {
        updateTaskList()
    }

    fun updateTaskList() {
        manager.loadProcessingTaskList()
    }

    fun onClickFilter() {
        navigator.openSearchTaskScreen()
    }

}