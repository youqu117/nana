package com.pixelpet.focuslock

import android.graphics.Paint
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.pixelpet.R

class TodoActivity : AppCompatActivity() {

    private lateinit var repository: TodoRepository
    private lateinit var adapter: TodoAdapter
    private val todoList = mutableListOf<TodoItem>()
    private lateinit var tvTodoSummary: TextView
    private lateinit var tvTodoEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)


        repository = TodoRepository(this)
        todoList.addAll(repository.getTodos())


        val rvTodos = findViewById<RecyclerView>(R.id.rvTodos)
        val etNewTodo = findViewById<EditText>(R.id.etNewTodo)
        tvTodoSummary = findViewById(R.id.tvTodoSummary)
        tvTodoEmpty = findViewById(R.id.tvTodoEmpty)
        val btnAddTodo = findViewById<ImageButton>(R.id.btnAddTodo)
        val btnClearDone = findViewById<ImageButton>(R.id.btnClearDone)

        adapter = TodoAdapter(
            todoList,
            onCheck = { item ->
                item.isCompleted = !item.isCompleted
                repository.saveTodos(todoList)
                refreshTodoUi()
            },
            onDelete = { item ->
                todoList.remove(item)
                repository.saveTodos(todoList)
                refreshTodoUi()
            },
            onFocus = { item ->
                if (item.isCompleted) {
                    Toast.makeText(this, "该任务已完成，请选择未完成任务", Toast.LENGTH_SHORT).show()
                    return@TodoAdapter
                }
                val data = Intent().apply {
                    putExtra(EXTRA_TODO_ID, item.id)
                    putExtra(EXTRA_TODO_TEXT, item.text)
                }
                setResult(RESULT_OK, data)
                Toast.makeText(this, "已选择：${item.text}", Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        rvTodos.layoutManager = LinearLayoutManager(this)
        rvTodos.adapter = adapter

        btnAddTodo.setOnClickListener {
            val text = etNewTodo.text.toString().trim()
            if (text.isNotEmpty()) {
                val newItem = TodoItem(text = text)
                todoList.add(newItem)
                repository.saveTodos(todoList)
                etNewTodo.text.clear()
                refreshTodoUi()
            } else {
                Toast.makeText(this, "任务内容不能为空", Toast.LENGTH_SHORT).show()
            }
        }

        btnClearDone.setOnClickListener {
            val before = todoList.size
            todoList.removeAll { it.isCompleted }
            if (todoList.size == before) {
                Toast.makeText(this, "没有可清理的已完成任务", Toast.LENGTH_SHORT).show()
            } else {
                repository.saveTodos(todoList)
                refreshTodoUi()
                Toast.makeText(this, "已清理已完成任务", Toast.LENGTH_SHORT).show()
            }
        }

        refreshTodoUi()
    }

    private fun refreshTodoUi() {
        val completed = todoList.count { it.isCompleted }
        val pending = todoList.size - completed
        tvTodoSummary.text = "总计 ${todoList.size} · 已完成 $completed · 待办 $pending"
        tvTodoEmpty.visibility = if (todoList.isEmpty()) View.VISIBLE else View.GONE
        adapter.notifyDataSetChanged()
    }

    companion object {
        const val EXTRA_TODO_ID = "extra_todo_id"
        const val EXTRA_TODO_TEXT = "extra_todo_text"
    }
}

class TodoAdapter(
    private val items: List<TodoItem>,
    private val onCheck: (TodoItem) -> Unit,
    private val onDelete: (TodoItem) -> Unit,
    private val onFocus: (TodoItem) -> Unit
) : RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvTodoText)
        val tvHint: TextView = view.findViewById(R.id.tvTodoHint)
        val ivCheck: ImageView = view.findViewById(R.id.ivCheck)

        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
        val container: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvText.text = item.text
        
        if (item.isCompleted) {
            holder.ivCheck.alpha = 1.0f
            holder.ivCheck.setBackgroundResource(R.drawable.bg_status_icon_on)
            holder.tvText.paintFlags = holder.tvText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvText.alpha = 0.5f
            holder.tvHint.text = "已完成"
        } else {
            holder.ivCheck.alpha = 0.2f
            holder.ivCheck.setBackgroundResource(R.drawable.bg_status_icon_off)
            holder.tvText.paintFlags = holder.tvText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvText.alpha = 1.0f
            holder.tvHint.text = "点按完成 · 长按专注"
        }

        holder.container.setOnClickListener { onCheck(item) }
        holder.container.setOnLongClickListener {
            onFocus(item)
            true
        }
        holder.ivDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size
}

