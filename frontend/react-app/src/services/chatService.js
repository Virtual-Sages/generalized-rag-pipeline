const SIMULATED_DELAY_MS = 1500;

const DUMMY_REPLIES = [
  "I am analyzing your request...",
  "Let me look into that for you.",
  "Here's what I found based on your query.",
  "That's an interesting question — give me a moment.",
];

class ChatService {
  // Dummy implementation: backend POST /api/message isn't implemented yet
  // (separate story), so simulate the round trip locally for now.
  // Once the endpoint exists, swap this body for:
  //
  // import makeHttpRequest from "./httpService";
  //
  // static sendMessage(chatId, prompt) {
  //   return makeHttpRequest({
  //     method: "POST",
  //     url: "/message",
  //     data: { chatId, prompt },
  //   });
  // }
  // eslint-disable-next-line no-unused-vars -- prompt is unused by the dummy body but kept in the signature to match the real API shape
  static sendMessage(chatId, prompt) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const reply = DUMMY_REPLIES[Math.floor(Math.random() * DUMMY_REPLIES.length)];
        resolve({
          id: Date.now(),
          chatId: chatId ?? crypto.randomUUID(),
          role: "ASSISTANT",
          content: reply,
          createdAt: new Date().toISOString(),
        });
      }, SIMULATED_DELAY_MS);
    });
  }
}

export default ChatService;
