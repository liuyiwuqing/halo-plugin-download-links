export type PresetItem = {
  label: string;
  value: string;
  icon: string;
};

export const ATTACHMENT_SOURCE = "附件";
export const ATTACHMENT_ICON = "/plugins/download-links/assets/static/icon/attachment.svg";

export function withBuiltinAttachmentPreset(sourcePresets: PresetItem[]): PresetItem[] {
  return [
    ...sourcePresets.filter(source => source.value !== ATTACHMENT_SOURCE),
    {label: ATTACHMENT_SOURCE, value: ATTACHMENT_SOURCE, icon: ATTACHMENT_ICON},
  ];
}
