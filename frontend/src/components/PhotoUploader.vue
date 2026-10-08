<script setup>
import { ref } from 'vue'
import { uploadFile } from '@/api/file'

// 父组件传进来的数据（只读！）
const props = defineProps({
  label: { type: String, required: true },
  tip: { type: String, default: '' },
  modelValue: { type: String, default: '' }
})

// 我可以向父组件"上报"的事件
const emit = defineEmits(['update:modelValue'])

const uploading = ref(false)
const inputRef = ref(null)

function choose() {
  inputRef.value.click()
}

async function onChange(event) {
  const file = event.target.files[0]
  if (!file) return

  uploading.value = true
  try {
    const url = await uploadFile(file)
    // 关键：不是直接改，而是"上报"给父组件
    emit('update:modelValue', url)
  } catch (e) {
    alert(e.message || '上传失败')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}
</script>

<template>
  <div class="uploader">
    <div class="label">{{ label }}</div>

    <div class="box" :class="{ filled: modelValue }" @click="choose">
      <img v-if="modelValue" :src="modelValue" alt="已上传" />
      <div v-else class="placeholder">
        <span class="plus">+</span>
        <span class="text">{{ uploading ? '上传中...' : '点击上传' }}</span>
      </div>
    </div>

    <p v-if="tip" class="tip">{{ tip }}</p>

    <input
        ref="inputRef"
        type="file"
        accept="image/*"
        class="hidden"
        @change="onChange"
    />
  </div>
</template>

<style scoped>
.uploader { flex: 1; }

.label {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.box {
  height: 200px;
  border: 2px dashed #d9d9d9;
  border-radius: 12px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.25s;
}

.box:hover { border-color: #5b7fff; background: #f0f5ff; }
.box.filled { border-style: solid; border-color: #e5e7eb; background: #fff; }

.box img { width: 100%; height: 100%; object-fit: cover; }

.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #999;
}

.plus { font-size: 32px; line-height: 1; color: #c0c4cc; }
.text { font-size: 14px; }

.tip { font-size: 12px; color: #bbb; margin-top: 8px; }

.hidden { display: none; }
</style>