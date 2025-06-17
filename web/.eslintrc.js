module.exports = {
  root: true,
  env: {
    node: true,
    browser: true,
  },
  extends: ["plugin:vue/essential", "eslint:recommended"],
  parserOptions: {
    parser: "babel-eslint",
  },
  rules: {
    // Disable or downgrade problematic rules
    "vue/require-prop-types": "off",
    "vue/require-default-prop": "off",
    "vue/no-unused-vars": "warn",
    "no-unused-vars": "warn",
    "no-undef": "warn",
    eqeqeq: "warn",
    "no-return-assign": "warn",
    "new-cap": "warn",
    "vue/html-self-closing": "off",
    "vue/html-closing-bracket-spacing": "off",
    "vue/this-in-template": "off",
    "vue/require-v-for-key": "warn",
    "vue/valid-v-model": "warn",
    "vue/attributes-order": "off",
    "no-multiple-empty-lines": "warn",

    // Style rules - make them warnings instead of errors
    quotes: ["warn", "single"],
    "comma-dangle": ["warn", "never"],
    "space-in-parens": "warn",
    "comma-spacing": "warn",
    "object-curly-spacing": ["warn", "always"],
    "arrow-spacing": "warn",
    semi: ["warn", "never"],
    "no-multi-spaces": "warn",

    // Turn off console warnings for development
    "no-console": process.env.NODE_ENV === "production" ? "warn" : "off",
    "no-debugger": process.env.NODE_ENV === "production" ? "warn" : "off",
  },
  globals: {
    // Define global variables to prevent 'undefined' errors
    ZLMRTCClient: "readonly",
    jessibuca: "readonly",
  },
}
