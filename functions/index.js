/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const { defineSecret } = require("firebase-functions/params");
const { onRequest } = require("firebase-functions/v2/https");
const axios = require("axios");

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

module.exports.getChatGPTResponse = onRequest(
  { secrets: [OPENAI_API_KEY] },
  async (req, res) => {
    try {
      console.log("Incoming Request Body:", JSON.stringify(req.body)); // ✅ Log incoming request

      const openaiApiKey = OPENAI_API_KEY.value();
      if (!openaiApiKey) {
        throw new Error("OpenAI API key is missing.");
      }

      const { messages } = req.body;
      if (!messages || !Array.isArray(messages)) {
        console.error("Error: 'messages' must be an array");
        return res.status(400).json({ error: "'messages' field is required and must be an array" });
      }

      console.log("Valid Request Format");

      // Call OpenAI API
      const response = await axios.post(
        "https://api.openai.com/v1/chat/completions",
        {
          model: "gpt-3.5-turbo",
          messages,
          max_tokens: 100,
        },
        {
          headers: {
            Authorization: `Bearer ${openaiApiKey}`,
            "Content-Type": "application/json",
          },
        }
      );

      console.log("OpenAI Response:", JSON.stringify(response.data)); // ✅ Log OpenAI response

      if (!response.data || !response.data.choices) {
        throw new Error("OpenAI API response missing 'choices'");
      }

      res.status(200).json(response.data);

    } catch (error) {
      console.error("Error in Firebase Function:", error.message);
      res.status(500).json({ error: error.message });
    }
  }
);









// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
