import { generate } from 'openapi-typescript-codegen'
import axios from 'axios'
import fs from 'fs/promises'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const DEFAULT_SPEC_URL = 'http://localhost:8080/admin-api/v3/api-docs'
const specUrl = process.env.OPENAPI_SPEC_URL || DEFAULT_SPEC_URL
const outputDir =
  process.env.SDK_OUTPUT_DIR || path.resolve(__dirname, '../src/api/generated')
const tempFile = path.resolve(__dirname, '../.openapi-temp.json')

const token = process.env.OPENAPI_TOKEN

async function main() {
  console.log(`[sdk] Fetching OpenAPI spec from ${specUrl}`)
  const response = await axios.get(specUrl, {
    responseType: 'json',
    timeout: 30000,
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  })

  await fs.writeFile(tempFile, JSON.stringify(response.data, null, 2), 'utf8')

  // 清理旧的生成内容
  await fs.rm(outputDir, { recursive: true, force: true })

  console.log('[sdk] Generating TypeScript client...')
  await generate({
    input: tempFile,
    output: outputDir,
    httpClient: 'fetch',
    useOptions: true,
    useUnionTypes: true,
    modular: true,
  })

  await fs.rm(tempFile, { force: true })
  console.log(`[sdk] Generation completed -> ${outputDir}`)
}

main().catch((error) => {
  console.error('[sdk] Generation failed:', error.message)
  process.exit(1)
})
