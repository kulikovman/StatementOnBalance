package com.lenta.bp15.features.task_card

import androidx.lifecycle.map
import com.lenta.bp15.model.ITaskManager
import com.lenta.bp15.model.pojo.Task
import com.lenta.bp15.platform.navigation.IScreenNavigator
import com.lenta.shared.platform.viewmodel.CoreViewModel
import com.lenta.shared.utilities.databinding.PageSelectionListener
import com.lenta.shared.utilities.extentions.launchUITryCatch
import com.lenta.shared.utilities.extentions.mapSkipNulls
import javax.inject.Inject

class TaskCardViewModel : CoreViewModel(), PageSelectionListener {

    @Inject
    lateinit var navigator: IScreenNavigator

    @Inject
    lateinit var manager: ITaskManager


    /**
    Переменные
     */

    private val task by lazy {
        manager.currentTask
    }

    val title by lazy {
        task.map { it.firstLine }
    }

    val isExistComment by lazy {
        task.map { it.comment.isNotEmpty() }
    }

    val taskInfo by lazy {
        task.mapSkipNulls { task ->
            task.convertToTaskInfoUi()
        }
    }

    /**
    Методы
     */

    override fun onPageSelected(position: Int) {
        selectedPage.value = position
    }

    fun onClickNext() {
        task.value?.let { task ->
            if (task.isEmptyGoodList()) {
                manager.loadContentToCurrentTask(::handleLoadContentSuccess)
            } else {
                navigator.openGoodListScreen()
            }
        }
    }

    private fun handleLoadContentSuccess() {
        navigator.openGoodListScreen()
    }

    fun onBackPressed() {
        task.value?.let { task ->
            if (task.isChanged()) {
                navigator.showTaskUnsentDataWillBeDeleted(task.firstLine) {
                    manager.prepareToUpdateTaskList()
                    unlockTask(task)
                }
            } else {
                unlockTask(task)
            }
        }
    }

    private fun unlockTask(task: Task) {
        launchUITryCatch {
            navigator.goBack()
            manager.unlockTask(task)
        }
    }

}