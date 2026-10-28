import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'

/**
 * ESLint 扁平配置（ESLint 9 / eslint.config.js）
 *
 * 最重要的一条规则是 no-undef：
 * 它能直接抓出「router is not defined」这类「变量没定义就用了」的错误 ——
 * 这类 bug 之前只能靠你在浏览器里手点才能发现。
 */
export default [
  {
    name: 'app/files-to-lint',
    files: ['**/*.{js,mjs,cjs,vue}']
  },
  {
    name: 'app/files-to-ignore',
    ignores: ['**/dist/**', '**/node_modules/**', '**/coverage/**', '**/.screenshots/**']
  },

  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],

  {
    name: 'app/rules',
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.es2021
      }
    },
    rules: {
      // 页面文件叫 LoginView / HomeView，不需要强行多词
      'vue/multi-word-component-names': 'off',

      // 未定义变量直接报错 —— 这是我们最想守住的那条线
      'no-undef': 'error',

      // 未使用的变量只提示，不阻断（很多是预留的）
      'no-unused-vars': ['warn', { args: 'none', caughtErrors: 'none' }],

      // 模板里的属性顺序、自闭合不强求
      'vue/attributes-order': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/html-self-closing': 'off',
      'vue/html-indent': 'off',
      'vue/html-closing-bracket-newline': 'off',

      // 可选 prop 不必强制给默认值（Vue 3 里 undefined 就是合理的"未传"）
      'vue/require-default-prop': 'off'
    }
  }
]
