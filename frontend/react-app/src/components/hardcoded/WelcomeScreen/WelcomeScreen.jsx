import React from 'react';
import './WelcomeScreen.scss';

const WelcomeScreen = ({ isVisible = true }) => {
  if (!isVisible) return null;

  return (
    <div className="welcome-screen">
      <div className="welcome-header">
        <h2>Welcome to AI Document Chat</h2>
        <p>Your intelligent assistant for analyzing complex documents and reports.</p>
      </div>

      <div className="guidelines-grid">
        <div className="guideline-card">
          <span className="material-symbols-outlined icon-blue">rule</span>
          <h3>Guidelines</h3>
          <p>Keep questions concise and reference specific pages or sections for the most accurate extraction.</p>
        </div>

        <div className="guideline-card">
          <span className="material-symbols-outlined icon-green">tips_and_updates</span>
          <h3>How to Use</h3>
          <p>Upload a document using the <strong>+</strong> button below, then ask questions about liabilities, tables, or summaries.</p>
        </div>

        <div className="guideline-card">
          <span className="material-symbols-outlined icon-orange">gavel</span>
          <h3>Limitations</h3>
          <p>The AI can make mistakes or misinterpret complex tables. Always verify critical financial figures manually.</p>
        </div>

        <div className="guideline-card">
          <span className="material-symbols-outlined icon-purple">privacy_tip</span>
          <h3>Privacy & Data</h3>
          <p>Your documents are encrypted. Do not upload files containing unredacted PII (Personally Identifiable Information).</p>
        </div>
      </div>
    </div>
  );
};

export default WelcomeScreen;