<script lang="ts" setup>
import {nodeViewProps, NodeViewWrapper} from "@halo-dev/richtext-editor";
import {computed, onMounted, ref, watch} from "vue";
import {consoleApiClient} from "@halo-dev/api-client";
import {utils} from "@halo-dev/ui-shared";
import MdiAddBold from '~icons/mdi/add-bold';
import MdiRemoveBold from '~icons/mdi/remove-bold';
import MdiPaperclip from "~icons/mdi/paperclip";
import {applyAttachmentToLinkItem, type AttachmentLike, type LinkItem} from "@/editor/attachment-link";
import {ATTACHMENT_SOURCE, type PresetItem, withBuiltinAttachmentPreset} from "@/editor/download-source";

const props = defineProps(nodeViewProps as any);

type DownloadSource = {
  name: string;
  icon: string;
};

const presets = ref<PresetItem[]>([]);
const attachmentSelectorVisible = ref(false);
const activeAttachmentItem = ref<LinkItem>();

async function loadPresets() {
  try {
    const config = await consoleApiClient.plugin.plugin.fetchPluginJsonConfig({name: "download-links"});
    const configData = config.data as any;
    const downloadSourceList: DownloadSource[] = configData?.basic?.downloadSourceList || [];

    presets.value = withBuiltinAttachmentPreset(downloadSourceList.map((source) => ({
      label: source.name,
      value: source.name,
      icon: source.icon
    })));
  } catch (error) {
    console.error("加载下载来源配置失败:", error);
    presets.value = withBuiltinAttachmentPreset([
      {label: "百度云网盘", value: "百度云网盘", icon: ""}
    ]);
  }
}

const links = ref<LinkItem[]>([]);

function initLinks() {
  const nodeLinks = props.node?.attrs?.links;
  if (Array.isArray(nodeLinks)) {
    links.value = JSON.parse(JSON.stringify(nodeLinks));
  } else {
    links.value = [];
  }
}

watch(
    () => props.node?.attrs?.links,
    (newLinks) => {
      if (newLinks !== undefined) {
        links.value = JSON.parse(JSON.stringify(newLinks || []));
      }
    },
    {deep: true}
);

initLinks();

function sync() {
  props.updateAttributes?.({links: JSON.parse(JSON.stringify(links.value))});
}

function addItem() {
  const defaultSource = presets.value.length > 0 ? presets.value[0].value : "";
  const defaultIcon = presets.value.length > 0 ? presets.value[0].icon : "";
  links.value.push({url: "", filename: "", source: defaultSource, code: "", icon: defaultIcon});
  sync();
}

function removeItem(index: number) {
  links.value.splice(index, 1);
  sync();
}

function onPresetChange(item: LinkItem) {
  const preset = presets.value.find(p => p.value === item.source);
  if (preset) {
    item.icon = preset.icon;
  }
  sync();
}

function getCurrentPreset(item: LinkItem) {
  return presets.value.find(p => p.value === item.source) || null;
}

function openAttachmentSelector(item: LinkItem) {
  activeAttachmentItem.value = item;
  attachmentSelectorVisible.value = true;
}

function closeAttachmentSelector() {
  attachmentSelectorVisible.value = false;
  activeAttachmentItem.value = undefined;
}

function onAttachmentSelect(attachments: AttachmentLike[]) {
  const attachment = attachments[0];
  if (activeAttachmentItem.value && attachment) {
    const attachmentPreset = presets.value.find(p => p.value === ATTACHMENT_SOURCE);
    applyAttachmentToLinkItem(
        activeAttachmentItem.value,
        attachment,
        (value) => utils.attachment.convertToSimple(value as Parameters<typeof utils.attachment.convertToSimple>[0]),
        attachmentPreset?.value,
        attachmentPreset?.icon,
    );
    sync();
  }
  closeAttachmentSelector();
}

const hasItems = computed(() => links.value.length > 0);

onMounted(() => {
  loadPresets();
});
</script>

<template>
  <node-view-wrapper as="div" class="downloadLinks">
    <div class="downloadLinks-header">
      <span>下载地址</span>
      <button class="downloadLinks-addBtn" @click="addItem" type="button">
        <MdiAddBold/>
        新增
      </button>
    </div>
    <div v-if="!hasItems" class="downloadLinks-empty">
      暂无下载项，点击上方按钮新增
    </div>
    <div v-for="(item, i) in links" :key="i" class="downloadLinks-item">
      <div class="downloadLinks-itemContent">
        <div class="downloadLinks-sourceWrapper">
          <img
              v-if="getCurrentPreset(item)?.icon"
              :src="getCurrentPreset(item)?.icon"
              :alt="item.source"
              class="downloadLinks-icon"
          />
          <div class="downloadLinks-iconPlaceholder" v-else></div>
          <select v-model="item.source" @change="onPresetChange(item)" class="downloadLinks-sourceSelect">
            <option v-for="p in presets" :key="p.value" :value="p.value">{{ p.label }}</option>
          </select>
        </div>
        <div class="downloadLinks-field">
          <label>文件名 <span class="required">*</span></label>
          <input
              v-model="item.filename"
              placeholder="例如：example.zip"
              @input="sync"
              required
              :class="{ 'input-error': !item.filename?.trim() }"
          />
        </div>
        <div class="downloadLinks-field downloadLinks-field-url">
          <label>下载地址 <span class="required">*</span></label>
          <div class="downloadLinks-urlInputGroup">
            <input
                v-model="item.url"
                placeholder="https://..."
                @input="sync"
                required
                :class="{ 'input-error': !item.url?.trim() }"
            />
            <button
                class="downloadLinks-attachmentBtn"
                @click="openAttachmentSelector(item)"
                type="button"
                title="选择附件"
            >
              <MdiPaperclip/>
              附件
            </button>
          </div>
        </div>
        <div class="downloadLinks-field downloadLinks-field-code">
          <label>提取码</label>
          <input v-model="item.code" placeholder="可选" @input="sync"/>
        </div>
        <button class="downloadLinks-deleteBtn" @click="removeItem(i)" type="button" title="删除">
          <MdiRemoveBold/>
        </button>
      </div>
    </div>
    <AttachmentSelectorModal
        v-model:visible="attachmentSelectorVisible"
        :accepts="['*/*']"
        :min="1"
        :max="1"
        @close="closeAttachmentSelector"
        @select="onAttachmentSelect"
    />
  </node-view-wrapper>
</template>

<style scoped>
.downloadLinks {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px;
  margin: 8px 0;
  background: #fafafa;
}

.downloadLinks-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  margin-bottom: 6px;
}

.downloadLinks-header span {
  font-size: 13px;
  color: #111827;
  font-weight: 600;
}

.downloadLinks-addBtn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  color: #374151;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.downloadLinks-addBtn:hover {
  background: #e5e7eb;
  border-color: #d1d5db;
}

.downloadLinks-empty {
  color: #6b7280;
  font-size: 12px;
  padding: 12px 8px;
  text-align: center;
}

.downloadLinks-item {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 6px;
}

.downloadLinks-item:last-child {
  margin-bottom: 0;
}

.downloadLinks-itemContent {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 6px;
}

.downloadLinks-sourceWrapper {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  margin-bottom: 0;
}

.downloadLinks-icon {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  object-fit: cover;
  flex-shrink: 0;
}

.downloadLinks-iconPlaceholder {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: #f3f4f6;
  flex-shrink: 0;
}

.downloadLinks-sourceSelect {
  padding: 0 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  color: #374151;
  min-width: 100px;
  flex-shrink: 0;
  height: 28px;
  box-sizing: border-box;
  line-height: 28px;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%236b7280' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  padding-right: 28px;
  transition: all 0.2s;
}

.downloadLinks-sourceSelect:hover {
  border-color: #9ca3af;
}

.downloadLinks-sourceSelect:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.downloadLinks-sourceSelect option {
  padding: 4px 8px;
  line-height: 1.5;
}

.downloadLinks-deleteBtn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 4px;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  margin-left: auto;
}

.downloadLinks-deleteBtn:hover {
  background: #fee2e2;
  color: #dc2626;
}

.downloadLinks-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1 1 auto;
  min-width: 120px;
}

.downloadLinks-field-url {
  flex: 2 1 auto;
  min-width: 180px;
}

.downloadLinks-field-code {
  flex: 0 1 auto;
  min-width: 80px;
}

.downloadLinks-field label {
  font-size: 11px;
  color: #6b7280;
  font-weight: 500;
  white-space: nowrap;
}

.downloadLinks-field input {
  padding: 5px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  color: #111827;
  transition: all 0.2s;
  width: 100%;
  min-width: 0;
  height: 28px;
  box-sizing: border-box;
}

.downloadLinks-field input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.downloadLinks-field input::placeholder {
  color: #9ca3af;
}

.downloadLinks-field .required {
  color: #ef4444;
  margin-left: 2px;
}

.downloadLinks-field input.input-error {
  border-color: #ef4444;
}

.downloadLinks-field input.input-error:focus {
  border-color: #ef4444;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.1);
}

.downloadLinks-urlInputGroup {
  display: flex;
  align-items: center;
  gap: 6px;
}

.downloadLinks-urlInputGroup input {
  flex: 1;
}

.downloadLinks-attachmentBtn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 28px;
  padding: 0 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  color: #374151;
  font-size: 12px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.downloadLinks-attachmentBtn:hover {
  border-color: #9ca3af;
  background: #f9fafb;
}
</style>
