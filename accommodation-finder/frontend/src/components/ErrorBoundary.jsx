import { Component } from 'react';

/**
 * Last line of defence around the routed content.
 *
 * A render-time exception in one page used to blank the entire document,
 * navbar included, leaving no way back except the browser's reload button.
 * This keeps the shell alive and offers the two things that actually help:
 * retry the same route, or go home.
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error('Unhandled error while rendering', error, info?.componentStack);
  }

  componentDidUpdate(previousProps) {
    // A successful navigation should clear the error, otherwise the fallback
    // would stick around for the rest of the session.
    if (this.state.error && previousProps.resetKey !== this.props.resetKey) {
      this.setState({ error: null });
    }
  }

  render() {
    const { error } = this.state;
    if (!error) return this.props.children;

    return (
      <main className="page page--narrow">
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">
            &#9888;
          </div>
          <h1>Something broke on this page</h1>
          <p className="muted">
            The rest of the app is still fine. Try this page again, or head back to search.
          </p>
          <p className="small muted">{error.message}</p>
          <div className="row" style={{ justifyContent: 'center' }}>
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => this.setState({ error: null })}
            >
              Try again
            </button>
            <a className="btn btn--ghost" href="/">
              Back to search
            </a>
          </div>
        </div>
      </main>
    );
  }
}
