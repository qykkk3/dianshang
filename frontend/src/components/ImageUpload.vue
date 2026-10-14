<template>
  <div class="image-upload">
    <div class="preview-list">
      <div v-for="(url, index) in urlList" :key="url + index" class="preview-item">
        <el-image :src="url" fit="cover" class="preview-image" />
        <div class="preview-mask">
          <el-icon class="mask-btn" @click="handlePreview(url)"><ZoomIn /></el-icon>
          <el-icon class="mask-btn" @click="handleRemove(index)"><Delete /></el-icon>
        </div>
      </div>

      <el-upload
        v-if="urlList.length < limit"
        class="upload-trigger"
        :show-file-list="false"
        :before-upload="handleBeforeUpload"
        accept="image/*"
        :disabled="uploading"
      >
        <div class="upload-box" :class="{ loading: uploading }">
          <el-icon><Plus /></el-icon>
          <span>{{ uploading ? '上传中...' : '上传图片' }}</span>
        </div>
      </el-upload>
    </div>

    <div v-if="tip" class="upload-tip">{{ tip }}</div>

    <el-dialog v-model="previewVisible" width="600px" title="图片预览" append-to-body>
      <img :src="previewUrl" style="width: 100%" alt="预览" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/admin/product'

/**
 * 图片上传组件（上传到后端本地磁盘并回显）
 *
 * 用法：
 *   单图：<ImageUpload v-model="form.mainImage" type="product" tip="建议尺寸 800x800" />
 *   多图：<ImageUpload v-model="form.images" type="product" multiple :limit="5" />
 */
const props = defineProps({
  modelValue: { type: [String, Array], default: '' },
  // 上传目录分类：product / banner / avatar
  type: { type: String, default: 'product' },
  multiple: { type: Boolean, default: false },
  limit: { type: Number, default: 5 },
  tip: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const uploading = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')

const urlList = computed(() => {
  if (!props.modelValue) return []
  if (Array.isArray(props.modelValue)) return props.modelValue.filter(Boolean)
  return [props.modelValue]
})

const applyChange = (list) => {
  if (props.multiple) {
    emit('update:modelValue', list)
  } else {
    emit('update:modelValue', list.length ? list[0] : '')
  }
}

const handleBeforeUpload = async (file) => {
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  uploading.value = true
  try {
    const res = await uploadFile(file, props.type)
    const url = res.data.url
    if (props.multiple) {
      applyChange([...urlList.value, url])
    } else {
      applyChange([url])
    }
    ElMessage.success('上传成功')
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    uploading.value = false
  }
  // 阻止 el-upload 的默认上传行为
  return false
}

const handleRemove = (index) => {
  const list = [...urlList.value]
  list.splice(index, 1)
  applyChange(list)
}

const handlePreview = (url) => {
  previewUrl.value = url
  previewVisible.value = true
}
</script>

<style scoped>
.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.preview-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.preview-image {
  width: 100%;
  height: 100%;
  display: block;
}

.preview-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}

.preview-item:hover .preview-mask {
  opacity: 1;
}

.mask-btn {
  color: #fff;
  font-size: 18px;
  cursor: pointer;
}

.upload-box {
  width: 100px;
  height: 100px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #909399;
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.upload-box:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-box.loading {
  cursor: not-allowed;
  opacity: 0.7;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
