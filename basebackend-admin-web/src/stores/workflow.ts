import { create } from 'zustand'
import type {
  ProcessDefinition,
  ProcessInstance,
  Task,
  FormTemplate,
} from '@/types/workflow'

interface WorkflowState {
  // 流程定义
  processDefinitions: ProcessDefinition[]
  currentProcessDefinition: ProcessDefinition | null
  setProcessDefinitions: (definitions: ProcessDefinition[]) => void
  setCurrentProcessDefinition: (definition: ProcessDefinition | null) => void

  // 流程实例
  processInstances: ProcessInstance[]
  currentProcessInstance: ProcessInstance | null
  setProcessInstances: (instances: ProcessInstance[]) => void
  setCurrentProcessInstance: (instance: ProcessInstance | null) => void

  // 任务
  tasks: Task[]
  currentTask: Task | null
  pendingTaskCount: number
  setTasks: (tasks: Task[]) => void
  setCurrentTask: (task: Task | null) => void
  setPendingTaskCount: (count: number) => void

  // 表单模板
  formTemplates: FormTemplate[]
  currentFormTemplate: FormTemplate | null
  setFormTemplates: (templates: FormTemplate[]) => void
  setCurrentFormTemplate: (template: FormTemplate | null) => void

  // 表单数据
  formData: Record<string, any>
  setFormData: (data: Record<string, any>) => void
  clearFormData: () => void

  // 流程设计器状态
  bpmnXml: string | null
  setBpmnXml: (xml: string | null) => void

  // 加载状态
  loading: boolean
  setLoading: (loading: boolean) => void

  // 重置状态
  reset: () => void
}

export const useWorkflowStore = create<WorkflowState>((set) => ({
  // 流程定义
  processDefinitions: [],
  currentProcessDefinition: null,
  setProcessDefinitions: (definitions) => set({ processDefinitions: definitions }),
  setCurrentProcessDefinition: (definition) =>
    set({ currentProcessDefinition: definition }),

  // 流程实例
  processInstances: [],
  currentProcessInstance: null,
  setProcessInstances: (instances) => set({ processInstances: instances }),
  setCurrentProcessInstance: (instance) =>
    set({ currentProcessInstance: instance }),

  // 任务
  tasks: [],
  currentTask: null,
  pendingTaskCount: 0,
  setTasks: (tasks) => set({ tasks }),
  setCurrentTask: (task) => set({ currentTask: task }),
  setPendingTaskCount: (count) => set({ pendingTaskCount: count }),

  // 表单模板
  formTemplates: [],
  currentFormTemplate: null,
  setFormTemplates: (templates) => set({ formTemplates: templates }),
  setCurrentFormTemplate: (template) => set({ currentFormTemplate: template }),

  // 表单数据
  formData: {},
  setFormData: (data) => set({ formData: data }),
  clearFormData: () => set({ formData: {} }),

  // 流程设计器状态
  bpmnXml: null,
  setBpmnXml: (xml) => set({ bpmnXml: xml }),

  // 加载状态
  loading: false,
  setLoading: (loading) => set({ loading }),

  // 重置状态
  reset: () =>
    set({
      processDefinitions: [],
      currentProcessDefinition: null,
      processInstances: [],
      currentProcessInstance: null,
      tasks: [],
      currentTask: null,
      pendingTaskCount: 0,
      formTemplates: [],
      currentFormTemplate: null,
      formData: {},
      bpmnXml: null,
      loading: false,
    }),
}))
