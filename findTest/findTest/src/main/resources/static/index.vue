<template>
  <div>
    <div class="content_table" style="padding: 32px">
      <div class="number_box" style="margin-bottom: 32px;width: 800px;">
       <div class="nom">
         <div style="margin: 32px; margin-left: 0;">显示编号：</div>
         <div style="margin: 32px; margin-left: 0;color: red;text-align: center;font-size: 28px;">你的账户只有{{showNumber.length}}个</div>
        <div
          ref="myDiv"
          style="width: 150px"
          v-for="(item, index) in showNumber"
          :key="index"
        >
          <div class="number">{{ item }}</div>
        </div>
       </div>
       <div class="r_nom">{{adsProxyName}}</div>
      </div>
      <el-button type="success" @click="getNumber()">提取编号</el-button>
      <el-button type="warning" @click="copyNumber()">复制编号</el-button>
      <el-button type="warning" @click="clearInput()">一键清空输入框</el-button>
      <div style="margin-top: 32px; font-size: 14px;color: chocolate;">
        把你的账户复制出来放到里面就可以了
      </div>
      <el-input
        style="margin-top: 32px;width: 1000px;"
        type="textarea"
        :autosize="{ minRows: 20, maxRows: 20 }"
        placeholder="请输入内容"
        v-model="textarea2"
      >
      </el-input>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      input: '',
      textarea2: '',
      showNumber: '',
      adsProxyName:''
    }
  },

  methods: {
    clearInput() {
      this.textarea2 = ''
    },
    copyNumber() {
      // 选择所有具有相同类名的div元素
      const divs = document.querySelectorAll('.number') // 替换为实际的类名选择器

      // 提取每个div中的文本，并用换行符分隔
      const texts = Array.from(divs)
        .map((div) => div.textContent.trim())
        .join('\n') // 使用 '\n\n' 作为段落分隔符

      // 创建一个临时的textarea元素
      const textarea = document.createElement('textarea')
      textarea.value = texts
      document.body.appendChild(textarea)
      textarea.select()
      textarea.setSelectionRange(0, 99999) // For mobile devices

      try {
        // 执行复制操作
        document.execCommand('copy')
        console.log('文本已复制到剪贴板')
        this.$message({
          type: 'success',
          message: '文本已复制到剪贴板!',
        })
      } catch (err) {
        console.error('复制失败:', err)
        this.$message({
          type: 'error',
          message: '复制失败!',
        })
      }

      // 移除临时的textarea元素
      document.body.removeChild(textarea)
    },
    getNumber() {
      const lines = this.textarea2.split('\n')
      // Initialize arrays to hold the numbers and their statuses
      const numbers = []
      const statuses = []

      // Iterate through lines and extract numbers and their statuses
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim()
        console.log('line:', line)
        // Check if line starts with "编号："
        if (line.startsWith('编号：')) {
          // Extract the number after "编号："
          const number = line.substring(3).trim()
          numbers.push(number)
        }
        // Check if line contains "使用中" or "已停用"
        if (line === '使用中' || line === '已停用') {
          statuses.push(line)
        }
      }

      // Initialize an array to hold the valid numbers
      const validNumbers = []

      // Match each number with its status
      for (let i = 0; i < numbers.length; i++) {
        if (statuses[i] === '使用中') {
          validNumbers.push(numbers[i])
        }
      }

      // Join the valid numbers into a single string with space as the separator
      this.showNumber = validNumbers
    },
  },
}
</script>
